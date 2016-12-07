/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import play.Play;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Internal;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.hook.HooksManager;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.NoTransactionException;
import com.baasbox.exception.RoleAlreadyExistsException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.ShuttingDownDBException;
import com.baasbox.exception.SwitchUserContextException;
import com.baasbox.exception.TransactionIsStillOpenException;
import com.baasbox.exception.UnableToExportDbException;
import com.baasbox.exception.UnableToImportDbException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.permissions.PermissionTagService;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.eaio.uuid.UUID;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTxPooled;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class DbHelper {

	private static final String SCRIPT_FILE_NAME = "db.sql";
	private static final String EMPTY_DB_FILE_NAME = "empty_db_exported.json";
	private static final String CONFIGURATION_FILE_NAME = "configuration.conf";

	private static volatile boolean dbFreeze = false;

	private static ThreadLocal<Integer> tranCount = new ThreadLocal<Integer>() {
		protected Integer initialValue() {
			return 0;
		};
	};

	private static ThreadLocal<String> appcode = new ThreadLocal<String>() {
		protected String initialValue() {
			return "";
		};
	};

	private static ThreadLocal<Map<String,String>> transientRidsInTransaction = new ThreadLocal<Map<String,String>>() {
		protected Map<String,String> initialValue() {
			return Maps.newHashMap();
		};
	};
	
	public static String getRIDfromCurrentTransaction(String id){
		if (isInTransaction())	return transientRidsInTransaction.get().get(id);
		else return null;
	}
	
	public static void setRIDinCurrentTransaction(String id, String rid){
		if (isInTransaction()) transientRidsInTransaction.get().put(id, rid);
	}
	
	private static ThreadLocal<String> username = new ThreadLocal<String>() {
		protected String initialValue() {
			return "";
		};
	};

	private static ThreadLocal<String> password = new ThreadLocal<String>() {
		protected String initialValue() {
			return "";
		};
	};

	private static final String fetchPlan = "*:?";


	public static BigInteger getDBTotalSize(){
		BigInteger toRet;
		if (BBConfiguration.getInstance().isConfiguredDBLocal()){
			toRet=FileUtils.sizeOfDirectoryAsBigInteger(new File (BBConfiguration.getInstance().getDBFullPath()));
		}else {
			toRet=BigInteger.ZERO;
		}
		return toRet;
	}
	
	public static BigInteger getDBStorageFreeSpace(){
		if (BBConfiguration.getInstance().getDBSizeThreshold()!=BigInteger.ZERO) return BBConfiguration.getInstance().getDBSizeThreshold();
		return BigInteger.valueOf(new File(BBConfiguration.getInstance().getDBFullPath()).getFreeSpace());
	}

	
	public static String getCurrentAppCode(){
		return appcode.get();
	}
	
	public static String currentUsername(){
		return username.get();
	}

	public static boolean isInTransaction() {
		ODatabaseRecordTx db = getConnection();
		return db.getTransaction().isActive();
	}

	public static void requestTransaction() {
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Request Transaction: transaction count -before-: "
					+ tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (!isInTransaction()) {
			if (BaasBoxLogger.isTraceEnabled())
				BaasBoxLogger.trace("Begin transaction");
			db.begin();
			transientRidsInTransaction.get().clear();
		}
		tranCount.set(tranCount.get().intValue() + 1);
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Request Transaction: transaction count -after-: "
					+ tranCount.get());
	}

	public static void commitTransaction() {
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Commit Transaction: transaction count -before-: "
					+ tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (isInTransaction()) {

			if (BaasBoxLogger.isDebugEnabled())
				BaasBoxLogger.debug("Commit transaction");
			tranCount.set(tranCount.get().intValue() - 1);
			if (tranCount.get() < 0)
				throw new RuntimeException("Commit without transaction!");
			if (tranCount.get() == 0) {
				db.commit();
				db.getTransaction().close();
				transientRidsInTransaction.get().clear();
			}
		} else
			throw new NoTransactionException(
					"There is no open transaction to commit");
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Commit Transaction: transaction count -after-: "
					+ tranCount.get());
		
	}

	public static void rollbackTransaction() {
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Rollback Transaction: transaction count -before-: "
					+ tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (isInTransaction()) {
			if (BaasBoxLogger.isDebugEnabled())
				BaasBoxLogger.debug("Rollback transaction");
			db.getTransaction().rollback();
			db.getTransaction().close();
			transientRidsInTransaction.get().clear();
			tranCount.set(0);
		}
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("Rollback Transaction: transaction count -after-: "
					+ tranCount.get());
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Rollback Transaction: transaction count -after-: " + tranCount.get());
	}

	public static String selectQueryBuilder(String from, boolean count,
			QueryParams criteria) {
		String ret;
		if (count || criteria.justCountTheRecords())
			ret = "select count(*) from ";
		else
			ret = "select " + criteria.getFields() + " from ";
		ret += from;
		if (criteria.getWhere() != null && !criteria.getWhere().equals("")) {
			ret += " where ( " + criteria.getWhere() + " )";
		}

		//patch for issue #469
		if (StringUtils.isEmpty(criteria.getWhere()) && getConnection() != null){
			final OUser user = getConnection().getUser();
			if (!UserService.isAnAdmin(user.getName())) {
				 ret += " where 1=1";
			}
		}
		if (!count && !StringUtils.isEmpty(criteria.getGroupBy())) {
			ret += " group by ( " + criteria.getGroupBy() + " )";
		}
		if (!count && criteria.getOrderBy() != null
				&& !criteria.getOrderBy().equals("")) {
			ret += " order by " + criteria.getOrderBy();
		}
		int skip = 0;
		if (!count && criteria.getPage() != null && criteria.getPage() != -1) {
			skip += (criteria.getPage() * criteria.getRecordPerPage());
		}
		if (!count && (criteria.getSkip() != null)) {
			skip += criteria.getSkip();
		}

		if (!count && skip != 0) {
			ret += " skip " + skip;
		}

		if (!count && criteria.getPage() != null && criteria.getPage() != -1) {
			ret += " limit " + (criteria.getRecordPerPage() + (criteria.isMoreEnabled() ? 1:0));
		}
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("queryBuilder: " + ret);
		return ret;
	}

	/***
	 * Prepares a select statement
	 * 
	 * @param from
	 *            the class to query
	 * @param count
	 *            if true, perform a count instead of to retrieve the records
	 * @param criteria
	 *            the criteria to apply in the 'where' clause of the select
	 * @return an OCommandRequest object ready to be passed to the
	 *         {@link #selectCommandExecute(com.orientechnologies.orient.core.command.OCommandRequest, Object[])}
	 *         (OCommandRequest, String[])} method
	 * @throws SqlInjectionException
	 *             If the query is not a select statement
	 */
	public static OCommandRequest selectCommandBuilder(String from,
			boolean count, QueryParams criteria) throws SqlInjectionException {
		ODatabaseRecordTx db = DbHelper.getConnection();
		OCommandRequest command = db.command(new OSQLSynchQuery<ODocument>(
				selectQueryBuilder(from, count, criteria)
				));
		if (!command.isIdempotent()) throw new SqlInjectionException();
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("commandBuilder: ");
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("  " + criteria.toString());
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("  " + command.toString());
		return command;
	}

	
		/***
		 * Executes a select eventually passing the parameters
		 * 
		 * @param command
		 * @param params
		 *            positional parameters
		 * @return the List of the record retrieved (the command MUST be a select)
		 */
		public static List<ODocument> selectCommandExecute(OCommandRequest command,
				Object[] params) {
			List<ODocument> result = selectCommandExecute(command, true, params);
			return result;
		}

		/***
		 * Executes a select eventually passing the parameters
		 * 
		 * @param command
		 * @param securityExecution 
		 * @param params
		 *            positional parameters
		 * @return the List of the record retrieved (the command MUST be a select)
		 */
		public static List<ODocument> selectCommandExecute(OCommandRequest command,
				boolean securityExecution, Object[] params) {
			if(securityExecution){
				DbHelper.filterOUserPasswords(true);
			}
			List<ODocument> queryResult = command.execute((Object[]) params);
			if (securityExecution) {
				DbHelper.filterOUserPasswords(false);
			}
			return queryResult;
		}

		public static List<ODocument> commandExecute(OCommandRequest command,
				boolean securityExecution, Object[] params) {
			if (securityExecution) {
				DbHelper.filterOUserPasswords(true);
			}
			List<ODocument> queryResult = command.execute((Object[]) params);
			if (securityExecution) {
				DbHelper.filterOUserPasswords(false);
			}
			return queryResult;
		}

		public static List<ODocument> commandExecute(OCommandRequest command,
				Object[] params) {
			List<ODocument> queryResult = commandExecute(command, true, params);
			return queryResult;
		}
	
	
	/***
	 * Used to perform delete(s) and update(s) operations
	 * @param command
	 * @param params
	 * @return
	 */
	public static Integer sqlCommandExecute(OCommandRequest command,
			Object[] params) {
		Integer updateQueryResult = command.execute((Object[]) params);
		return updateQueryResult;
	}
	

	/**
	 * Prepares the command API to execute an arbitrary SQL statement
	 * 
	 * @param theQuery
	 * @return
	 */
	public static OCommandRequest genericSQLStatementCommandBuilder(
			String theQuery) {
		ODatabaseRecordTx db = DbHelper.getConnection();
		OCommandRequest command = db.command(new OCommandSQL(theQuery));
		return command;
	}

	/***
	 * Executes a generic SQL command statements
	 * 
	 * @param command
	 *            the command to execute prepared by
	 *            {@link #genericSQLStatementCommandBuilder(String)}
	 * @param params
	 *            The positional parameters to pass to the statement
	 * @return
	 */
	public static Object genericSQLCommandExecute(OCommandRequest command,
			Object[] params) {
		Object queryResult = command.execute((Object[]) params);
		return queryResult;
	}

	/**
	 * Executes an arbitrary sql statement applying the positional parameters
	 * 
	 * @param statement
	 * @param params
	 * @return
	 */
	public static Object genericSQLStatementExecute(String statement,
			Object[] params) {
		OCommandRequest command = genericSQLStatementCommandBuilder(statement);
		BaasBoxLogger.debug("Command to execute: " + command.toString() );
		Object ret = genericSQLCommandExecute(command,params);
		return ret;
	}

	public static void shutdownDB(boolean repopulate) {
		try {
			// WE GET THE CONNECTION BEFORE SETTING THE SEMAPHORE
			final ODatabaseRecordTx db = getConnection();

			synchronized(DbHelper.class)  {
				if(!dbFreeze){
					dbFreeze = true;
				}
				if (BBConfiguration.getInstance().isConfiguredDBLocal()){
					db.drop();
					db.close();
					db.create();
					db.getLevel1Cache().clear();
					db.getLevel2Cache().clear();
					db.reload();
					db.getMetadata().reload();
					if (repopulate) {
						HooksManager.registerAll(db);
						setupDb();
					}
				}else{  //remote DB
					//when using remote ODB there is no way to drop or create a new database using an admin user
					//we can simulate drop() and create() importing a new empty ODB database
					InputStream is=null;
					if (Play.application().isProd())
						is = Play.application().resourceAsStream(EMPTY_DB_FILE_NAME);
					else
						is = new FileInputStream(Play.application().getFile(
								"conf/" + EMPTY_DB_FILE_NAME));
					ODatabaseDocumentTx dbd = new ODatabaseDocumentTx(db);
					ODatabaseImport oi = new ODatabaseImport(dbd, is, new OCommandOutputListener() {
						@Override
						public void onMessage(String m) {
							BaasBoxLogger.info("Restore db (for drop): " + m);
						}
					});
					startImport(db, oi);
					oi.close();
					evolveDB(db);
					HooksManager.registerAll(db);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}finally{
			synchronized(DbHelper.class)  {
				dbFreeze=false;
			}
		}

	}

	private static void startImport(final ODatabaseRecordTx db, ODatabaseImport oi) {
		oi.setPreserveRids(true);
		oi.setIncludeManualIndexes(true);
		oi.setUseLineFeedForRecords(true);
		oi.setPreserveClusterIDs(true);
		oi.setPreserveRids(true);
		oi.setDeleteRIDMapping(true);
		oi.setIncludeSchema(true);
		oi.setIncludeClusterDefinitions(true);
		db.getMetadata().getIndexManager().flush();
		db.getMetadata().getIndexManager().reload();
		Set<OIndex> indexesToRebuild = new HashSet<OIndex>();
		db.getMetadata().getIndexManager().getIndexes().stream().forEach(idx->{
			 if(idx.isAutomatic()) {
				 BaasBoxLogger.info("...dropping {} index",idx.getName());
				 db.getMetadata().getIndexManager().dropIndex(idx.getName());
				 indexesToRebuild.add(idx);
			 }
		});
		db.getMetadata().getIndexManager().flush();
		db.getMetadata().getIndexManager().reload();
		 
		BaasBoxLogger.info("...starting import procedure...");
		oi.importDatabase();
	}

	public static ODatabaseRecordTx getOrOpenConnection(String appcode,
			String username, String password) throws InvalidAppCodeException {
		ODatabaseRecordTx db = getConnection();
		if (db == null || db.isClosed())
			db = open(appcode, username, password);
		return db;
	}

	public static ODatabaseRecordTx getOrOpenConnectionWIthHTTPUsername()
			throws InvalidAppCodeException {
		ODatabaseRecordTx db = getConnection();
		if (db == null || db.isClosed())
			db = open((String) Http.Context.current().args.get("appcode"),
					getCurrentHTTPUsername(), getCurrentHTTPPassword());
		return db;
	}

	public static ODatabaseRecordTx openFromContext(Http.Context ctx)
			throws InvalidAppCodeException {
		String username = (String) ctx.args.get("username");
		String password = (String) ctx.args.get("password");
		String appcode = (String) ctx.args.get("appcode");
		return open(appcode, username, password);
	}

	public static ODatabaseRecordTx open(String appcode, String username,String password) throws InvalidAppCodeException {
		
		if (appcode==null || !appcode.equals(BBConfiguration.getInstance().configuration.getString(BBConfiguration.getInstance().APP_CODE)))
			throw new InvalidAppCodeException("Authentication info not valid or not provided: " + appcode + " is an Invalid App Code");
		if(dbFreeze){

			throw new ShuttingDownDBException();
		}

		String databaseName=BBConfiguration.getInstance().getDBStorageType() + ":" + BBConfiguration.getInstance().getDBFullPath();
		
		/* these will be necessary when BaasBox will support OrientDB clusters */
		/*
		Path currentRelativePath = Paths.get("");
		System.setProperty("ORIENTDB_HOME",currentRelativePath.toAbsolutePath().toString());
		String databaseName=currentRelativePath.toAbsolutePath().toString()+"/databases/baasbox";
		*/
		
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("opening connection on db: " + databaseName + " for "
					+ username);
		
		ODatabaseDocumentPool odp=ODatabaseDocumentPool.global();
		ODatabaseDocumentTxPooled conn=new ODatabaseDocumentTxPooled(odp, databaseName, username, password);

		HooksManager.registerAll(getConnection());
		DbHelper.appcode.set(appcode);
		DbHelper.username.set(username);
		DbHelper.password.set(password);

		return getConnection();
	}
	
	public static boolean isConnectionLocal(){
		return isConnectionLocal(getConnection());
	}

	private static boolean isConnectionLocal(ODatabaseRecordTx connection) {
		return connection.getStorage().getName().equals("plocal");
	}

	public static boolean isConnectedAsAdmin(boolean excludeInternal) {
		if (getConnection()==null) return false;
		OUser user = getConnection().getUser();
		boolean isAdminRole = UserService.isAnAdmin(user.getName());
		return excludeInternal ? isAdminRole
				&& !BBConfiguration.getInstance().getBaasBoxAdminUsername().equals(
						user.getName()) : isAdminRole;
	}


	public static ODatabaseRecordTx reconnectAsAdmin() {
		if (tranCount.get()>0) throw new SwitchUserContextException("Cannot switch to admin context within an open transaction");
		DbHelper.close(DbHelper.getConnection());
		try {
			return open(appcode.get(),
					BBConfiguration.getInstance().getBaasBoxAdminUsername(),
					BBConfiguration.getInstance().getBaasBoxAdminPassword());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}

	public static ODatabaseRecordTx reconnectAsAuthenticatedUser() {
		if (tranCount.get()>0) throw new SwitchUserContextException("Cannot switch to user context within an open transaction");
		DbHelper.close(DbHelper.getConnection());
		try {
			return open(appcode.get(), getCurrentHTTPUsername(),
					getCurrentHTTPPassword());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}

	public static void close(ODatabaseRecordTx db) {
		if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("closing connection");
		if (db != null && !db.isClosed()) {
			// HooksManager.unregisteredAll(db);
			try {
				if (tranCount.get() != 0){
					rollbackTransaction();
					throw new TransactionIsStillOpenException(
							"Closing a connection with an active transaction: "
									+ tranCount.get() + ". Transaction will be rolled back");
				}
			} finally {
				db.close();
				tranCount.set(0);
			}
		} else if (BaasBoxLogger.isDebugEnabled())
			BaasBoxLogger.debug("connection already close or null");
	}

	public static ODatabaseRecordTx getConnection() {
		ODatabaseRecordTx db = null;
		try {
			db = (ODatabaseRecordTx) ODatabaseRecordThreadLocal.INSTANCE.get();
			if (BaasBoxLogger.isTraceEnabled())
				BaasBoxLogger.trace("Connection id: " + db + " "
						+ ((Object) db).hashCode());
		} catch (ODatabaseException e) {
			BaasBoxLogger.debug("Cound not retrieve the DB connection within this thread: "
					+ ExceptionUtils.getMessage(e));

		}
		return db;
	}

	public static String getCurrentHTTPPassword() {
		return (String) Http.Context.current().args.get("password");
	}

	public static String getCurrentHTTPUsername() {
		return (String) Http.Context.current().args.get("username");
	}

	public static String getCurrentUserNameFromConnection() {
		return getConnection().getUser().getName();
	}

	public static boolean isConnectedLikeBaasBox() {
		return getCurrentHTTPUsername().equalsIgnoreCase(
				BBConfiguration.getInstance().getBaasBoxUsername());
	}


	public static void createDefaultRoles() throws RoleNotFoundException, RoleAlreadyExistsException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		RoleService.createInternalRoles();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}

	public static void createDefaultUsers() throws Exception{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		UserService.createDefaultUsers();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}

	
	public static void updateDefaultUsers() throws Exception{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		UserDao udao = UserDao.getInstance();
		OUser user = udao.getOUserByUsername(BBConfiguration.getInstance().getBaasBoxUsername());
		user.setPassword(BBConfiguration.getInstance().getBaasBoxPassword());
		user.save();
		user = udao.getOUserByUsername(BBConfiguration.getInstance().getBaasBoxAdminUsername());
		user.setPassword(BBConfiguration.getInstance().getBaasBoxAdminPassword());
		user.save();

		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}

	public static void populateDB() throws IOException {
		ODatabaseRecordTx db = getConnection();
		//DO NOT DELETE THE FOLLOWING LINE!
		OrientGraphNoTx dbg =  new OrientGraphNoTx(getODatabaseDocumentTxConnection()); 
		BaasBoxLogger.info("Populating the db...");
		InputStream is;
		if (Play.application().isProd())
			is = Play.application().resourceAsStream(SCRIPT_FILE_NAME);
		else
			is = new FileInputStream(Play.application().getFile(
					"conf/" + SCRIPT_FILE_NAME));
		List<String> script = IOUtils.readLines(is, "UTF-8");
		is.close();

		for (String line:script){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		
		Internal.DB_VERSION._setValue(BBConfiguration.getInstance().getDBVersion());
		String uniqueId="";
		try{
			UUID u = new UUID();
			uniqueId = new String(Base64.encodeBase64(u.toString().getBytes()));
		} catch (Exception e) {
			java.util.UUID u = java.util.UUID.randomUUID();
			uniqueId = new String(Base64.encodeBase64(u.toString().getBytes()));
		}
		Internal.INSTALLATION_ID._setValue(uniqueId);
		BaasBoxLogger.info("Unique installation id is: " + uniqueId);
		BaasBoxLogger.info("...done");
	}

	public static void populateConfiguration () throws IOException, ConfigurationException{
		BaasBoxLogger.info("Load initial configuration...");
		InputStream is;
		if (Play.application().isProd())
			is = Play.application().resourceAsStream(CONFIGURATION_FILE_NAME);
		else
			is = new FileInputStream(Play.application().getFile(
					"conf/" + CONFIGURATION_FILE_NAME));
		HierarchicalINIConfiguration c = new HierarchicalINIConfiguration();
		c.setEncoding("UTF-8");
		c.load(is);
		CharSequence doubleDot = "..";
		CharSequence dot = ".";


		Set<String> sections= c.getSections();
		for (String section: sections){
			Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
			if (en==null){
				BaasBoxLogger.warn(section  + " is not a valid configuration section, it will be skipped!");
				continue;
			}
			SubnodeConfiguration subConf = c.getSection(section);
			Iterator<String> it = subConf.getKeys();
			while (it.hasNext()) {
				String key = (it.next());
				Object value = subConf.getString(key);
				key = key.replace(doubleDot, dot);// bug on the Apache library:
													// if the key contain a dot,
													// it will be doubled!
				try {
					BaasBoxLogger.info("Setting "+value+ " to "+key);
					PropertiesConfigurationHelper.setByKey(en, key, value);
				} catch (Exception e) {
					BaasBoxLogger.warn("Error loading initial configuration: Section " + section + ", key: " + key +", value: " + value, e);
				}
			}
		}
		is.close();
		BaasBoxLogger.info("...done");
	}


    static void createDefaultPermissionTags(){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        PermissionTagService.createDefaultPermissions();
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
    }

	public static void setupDb() throws Exception{
		BaasBoxLogger.info("Creating default roles...");
		DbHelper.createDefaultRoles();
		populateDB();
		getConnection().getMetadata().getIndexManager().reload();
		BaasBoxLogger.info("Creating default users...");
		createDefaultUsers();
		populateConfiguration();
		createDefaultPermissionTags();
	}

	public static void exportData(String appcode, OutputStream os)
			throws UnableToExportDbException {
		ODatabaseRecordTx db = null;

		try{
			db = open(appcode, BBConfiguration.getInstance().getBaasBoxAdminUsername(), BBConfiguration.getInstance().getBaasBoxAdminPassword());
			
			ODatabaseExport oe = new ODatabaseExport(db, os, new OCommandOutputListener() {
				@Override
				public void onMessage(String m) {
					BaasBoxLogger.info(m);
				}
			});

			oe.setUseLineFeedForRecords(true);
			oe.setIncludeManualIndexes(true);
			oe.exportDatabase();
			oe.close();
		} catch (Exception ioe) {
			throw new UnableToExportDbException(ioe);
		} finally {
			if (db != null && !db.isClosed()) {
				db.close();
			}
		}
	}

	
	public static void importData(String appcode,File newFile) throws UnableToImportDbException{
		if (newFile==null) throw new UnableToImportDbException("Cannot import file. The reference is null");
		final ODatabaseRecordTx db = getConnection();
		try{
			BaasBoxLogger.info("Initializing restore operation..:");
			BaasBoxLogger.info("...dropping the old db..:");

			synchronized(DbHelper.class)  {
				if(!dbFreeze){
					dbFreeze=true;
				}
			}
			//DbHelper.shutdownDB(false);
			
			BaasBoxLogger.info("...unregistering hooks...");
			HooksManager.unregisteredAll(db);
			BaasBoxLogger.info("...drop the O-Classes...");
			db.getMetadata().getSchema().dropClass("OFunction");

			 db.getMetadata().getSchema().dropClass("OSchedule");
			 db.getMetadata().getSchema().dropClass("ORIDs");
			ODatabaseDocumentTx dbd = new ODatabaseDocumentTx(db);
			ODatabaseImport oi = new ODatabaseImport(dbd, newFile.getAbsolutePath(), new OCommandOutputListener() {
				@Override
				public void onMessage(String m) {
					BaasBoxLogger.info("Restore db: " + m);
				}
			});
			 startImport(db, oi);
			 oi.close();
			 BaasBoxLogger.info("...setting up internal user credential...");
			 updateDefaultUsers();
			 BaasBoxLogger.info("...setting up DataBase attributes...");
			 setupAttributes();
			 BaasBoxLogger.info("...registering hooks...");
			 evolveDB(db);
			 HooksManager.registerAll(db);
			 BaasBoxLogger.info("...extract iOS certificates...");
			 IosCertificateHandler.init();
		}catch(Exception ioe){
			BaasBoxLogger.error("*** Error importing the db: ", ioe);
			throw new UnableToImportDbException(ioe);
		} finally {
			if (db != null && !db.isClosed()) {
				db.close();
			}
			BaasBoxLogger.info("...releasing the db...");
			dbFreeze=false;
			if(newFile!=null && newFile.exists()){
				newFile.delete();
			}
			BaasBoxLogger.info("...restore terminated");
		}
	}

	private static void setupAttributes() {
		ODatabaseRecordTx db = DbHelper.getConnection();
		DbHelper.execMultiLineCommands(db, BaasBoxLogger.isDebugEnabled(),
				"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.sssZ",
				"alter database custom useLightweightEdges=false",
				"alter database custom useClassForEdgeLabel=false",
				"alter database custom useClassForVertexLabel=true",
				"alter database custom useVertexFieldsForEdgeLabels=true");
	}

	/**
	 * Check the db level and evolve it
	 * 
	 * @param db
	 */
	public static void evolveDB(ODatabaseRecordTx db) {
		//check for evolutions
		 BaasBoxLogger.info("...looking for evolutions...");
		 String fromVersion="";

		 List<ODocument> qryResult = (List<ODocument>)DbHelper.genericSQLStatementExecute("select from (select expand(indexes) from metadata:indexmanager) where name = '_bb_internal'",null);
		 if (qryResult.size() > 0){
			 BaasBoxLogger.warn("...DB is < 0.7 ....");
			 qryResult = (List<ODocument>)DbHelper.genericSQLStatementExecute("select from index:_bb_internal where key = ?",new String[]{Internal.DB_VERSION.getKey()});
			 ODocument od = db.load((ORID)qryResult.get(0).field("rid",ORID.class));
			 BaasBoxLogger.debug("******: " + od);
			 fromVersion=od.field("value");
		 }else fromVersion=Internal.DB_VERSION.getValueAsString();
		 BaasBoxLogger.info("...db version is: " + fromVersion);
		 if (!fromVersion.equalsIgnoreCase(BBConfiguration.getInstance().getDBVersion())){
			 BaasBoxLogger.info("...imported DB needs evolutions!...");
			 Evolutions.performEvolutions(db, fromVersion);
			 Internal.DB_VERSION._setValue(BBConfiguration.getInstance().getDBVersion());
			 BaasBoxLogger.info("DB version is now " + BBConfiguration.getInstance().getDBVersion());
		 }//end of evolutions
	}

	public static OrientGraph getOrientGraphConnection() {
		return new OrientGraph(getODatabaseDocumentTxConnection(), false);
	}

	public static ODatabaseDocumentTx getODatabaseDocumentTxConnection() {
		return new ODatabaseDocumentTx(getConnection());
	}

	/**
	 * Executes a sequence of orient sql commands
	 */
    /**
     * Executes a sequence of orient sql commands
     */
    public static void execMultiLineCommands(ODatabaseRecordTx db,boolean log,boolean stopOnException,String ... commands){

    	BaasBoxLogger.debug("Ready to execute these commands: " + Arrays.toString(commands));
        if (commands==null) return;
        for (String command:commands){
            if (command==null){
                BaasBoxLogger.warn("null command found!! skipping");
                continue;
            }
            if (log)BaasBoxLogger.debug("sql:> "+command);
            if (!command.startsWith("--")&&!command.trim().isEmpty()){
            	if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Executing command: " + command);
            	try {
            		db.command(new OCommandSQL(command.replace(';',' '))).execute();
            	}catch(Throwable e){
            		if (stopOnException){
            			BaasBoxLogger.error("Exception during the statement execution: {}" ,ExceptionUtils.getFullStackTrace(e));
            			throw new RuntimeException(e);
            		}else{
            			BaasBoxLogger.warn("Exception during the statement execution: {}" ,ExceptionUtils.getMessage(e));
            		}
            	}
            }
        }
    }
    
	 /**
     * Executes a sequence of orient sql commands
     */
    public static void execMultiLineCommands(ODatabaseRecordTx db,boolean log,String ... commands){
    	execMultiLineCommands (db, log,true,commands);
    }
    
	public static void filterOUserPasswords(boolean activate) {
		HooksManager.enableHidePasswordHook(getConnection(), activate);
	}

	public static F.Function0<play.mvc.Result> withDbFromContext(Http.Context ctx, F.Function0<Result> work){
		return ()->{
			try{
				DbHelper.openFromContext(ctx);
				return work.apply();
			}catch (Throwable e){
                if (DbHelper.getConnection()!=null && ! DbHelper.getConnection().isClosed() && isInTransaction()) DbHelper.rollbackTransaction();
                throw e;
            }finally {
				DbHelper.close(DbHelper.getConnection());
			}
		};
	}


}
