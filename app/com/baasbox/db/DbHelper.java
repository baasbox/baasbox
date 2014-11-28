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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.Play;
import play.mvc.Http;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.configuration.Internal;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.dao.RoleDao;
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
import com.baasbox.service.permissions.PermissionTagService;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.eaio.uuid.UUID;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;



public class DbHelper {

	private static final String SCRIPT_FILE_NAME="db.sql";
	private static final String CONFIGURATION_FILE_NAME="configuration.conf";

	private static ThreadLocal<Boolean> dbFreeze = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {return Boolean.FALSE;};
	};
	private static ThreadLocal<Integer> tranCount = new ThreadLocal<Integer>() {
		protected Integer initialValue() {return 0;};
	};
	
	private static ThreadLocal<String> appcode = new ThreadLocal<String>() {
		protected String initialValue() {return "";};
	};

	private static ThreadLocal<String> username = new ThreadLocal<String>() {
		protected String initialValue() {return "";};
	};
	
	private static ThreadLocal<String> password = new ThreadLocal<String>() {
		protected String initialValue() {return "";};
	};
	
	private static final String fetchPlan = "*:?";

	public static BigInteger getDBTotalSize(){
		return FileUtils.sizeOfDirectoryAsBigInteger(new File (BBConfiguration.getDBDir()));
	}
	
	public static BigInteger getDBStorageFreeSpace(){
		if (BBConfiguration.getDBSizeThreshold()!=BigInteger.ZERO) return BBConfiguration.getDBSizeThreshold();
		return BigInteger.valueOf(new File(BBConfiguration.getDBDir()).getFreeSpace());
	}
	
		
	public static String currentUsername(){
		return username.get();
	}
	
	public static boolean isInTransaction(){
		 ODatabaseRecordTx db = getConnection();
		 return db.getTransaction().isActive();
	}

	public static void requestTransaction(){
		if (Logger.isDebugEnabled()) Logger.debug("Request Transaction: transaction count -before-: " + tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (!isInTransaction()){
			if (Logger.isTraceEnabled()) Logger.trace("Begin transaction");
			db.begin();
		}
		tranCount.set(tranCount.get().intValue()+1);
		if (Logger.isDebugEnabled()) Logger.debug("Request Transaction: transaction count -after-: " + tranCount.get());
	}

	public static void commitTransaction(){
		if (Logger.isDebugEnabled()) Logger.debug("Commit Transaction: transaction count -before-: " + tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (isInTransaction()){

			if (Logger.isDebugEnabled()) Logger.debug("Commit transaction");
			tranCount.set(tranCount.get().intValue()-1);
			if (tranCount.get()<0) throw new RuntimeException("Commit without transaction!");
			if (tranCount.get()==0) {
				db.commit();
				db.getTransaction().close();
			}	
		}else throw new NoTransactionException("There is no open transaction to commit");
		if (Logger.isDebugEnabled()) Logger.debug("Commit Transaction: transaction count -after-: " + tranCount.get());

	}

	public static void rollbackTransaction(){
		if (Logger.isDebugEnabled()) Logger.debug("Rollback Transaction: transaction count -before-: " + tranCount.get());
		ODatabaseRecordTx db = getConnection();
		if (isInTransaction()){
			if (Logger.isDebugEnabled()) Logger.debug("Rollback transaction");
			db.getTransaction().rollback();
			db.getTransaction().close();
			tranCount.set(0);
		}
		if (Logger.isDebugEnabled()) Logger.debug("Rollback Transaction: transaction count -after-: " + tranCount.get());
	}

	public static String selectQueryBuilder (String from, boolean count, QueryParams criteria){
		String ret;
		if (count || criteria.justCountTheRecords()) ret = "select count(*) from ";
		else ret = "select " + criteria.getFields() + " from ";
		ret += from;
		if (criteria.getWhere()!=null && !criteria.getWhere().equals("")){
			ret += " where ( " + criteria.getWhere() + " )";
		}
		//patch for issue #469
		if (StringUtils.isEmpty(criteria.getWhere())){
			ret += " where 1=1";
		}
		if (!StringUtils.isEmpty(criteria.getGroupBy())){
			ret += " group by ( " + criteria.getGroupBy() + " )";
		}
		if (!count && criteria.getOrderBy()!=null && !criteria.getOrderBy().equals("")){
			ret += " order by " + criteria.getOrderBy();
		}
		int skip=0;
		if (!count && criteria.getPage()!=null && criteria.getPage()!=-1 ){
			skip+=(criteria.getPage() * criteria.getRecordPerPage());
		}
		if (!count && (criteria.getSkip()!=null)){
			skip += 	criteria.getSkip();
		}
		
		if (skip!=0){
			ret+= " skip " + skip;
		}
		
		if (!count && criteria.getPage()!=null && criteria.getPage()!=-1 ){
			ret += 	" limit " + criteria.getRecordPerPage();
		}
		if (Logger.isDebugEnabled()) Logger.debug("queryBuilder: " + ret);
		return ret;
	}

	/***
	 * Prepares a select statement
	 * @param from the class to query
	 * @param count if true, perform a count instead of to retrieve the records
	 * @param criteria the criteria to apply in the 'where' clause of the select
	 * @return an OCommandRequest object ready to be passed to the {@link #selectCommandExecute(com.orientechnologies.orient.core.command.OCommandRequest, Object[])} (OCommandRequest, String[])} method
	 * @throws SqlInjectionException If the query is not a select statement
	 */
	public static OCommandRequest selectCommandBuilder(String from, boolean count, QueryParams criteria) throws SqlInjectionException{
		ODatabaseRecordTx db =  DbHelper.getConnection();
		OCommandRequest command = db.command(new OSQLSynchQuery<ODocument>(
				selectQueryBuilder(from, count, criteria)
				));
		if (!command.isIdempotent()) throw new SqlInjectionException();
		if (Logger.isDebugEnabled()) Logger.debug("commandBuilder: ");
		if (Logger.isDebugEnabled()) Logger.debug("  " + criteria.toString());
		if (Logger.isDebugEnabled()) Logger.debug("  " + command.toString());
		return command;
	}

	/***
	 * Executes a select eventually passing the parameters 
	 * @param command
	 * @param params positional parameters
	 * @return the List of the record retrieved (the command MUST be a select)
	 */
	public static List<ODocument> selectCommandExecute(OCommandRequest command, Object[] params){
		DbHelper.filterOUserPasswords(true);
		List<ODocument> queryResult = command.execute((Object[])params);
		DbHelper.filterOUserPasswords(false);
		return queryResult;
	}
	public static Integer sqlCommandExecute(OCommandRequest command, Object[] params){
		Integer updateQueryResult = command.execute((Object[])params);
		return updateQueryResult;
	}
	public static List<ODocument> commandExecute(OCommandRequest command, Object[] params){
		DbHelper.filterOUserPasswords(true);
        List<ODocument> queryResult = command.execute((Object[])params);
        DbHelper.filterOUserPasswords(false);
        return queryResult;
	}
	
	/**
	 * Prepares the command API to execute an arbitrary SQL statement
	 * @param theQuery
	 * @return
	 */
	public static OCommandRequest genericSQLStatementCommandBuilder (String theQuery){
		ODatabaseRecordTx db =  DbHelper.getConnection();
		OCommandRequest command = db.command(new OCommandSQL(theQuery));
		return command;
	}
	
	/***
	 * Executes a generic SQL command statements
	 * @param command the command to execute prepared by {@link #genericSQLStatementCommandBuilder(String)}
	 * @param params The positional parameters to pass to the statement
	 * @return
	 */
	public static Object genericSQLCommandExecute(OCommandRequest command, Object[] params){
		Object queryResult = command.execute((Object[])params);
		return queryResult;
	}
	
	/**
	 * Executes an arbitrary sql statement applying the positional parameters
	 * @param statement
	 * @param params
	 * @return
	 */
	public static Object genericSQLStatementExecute(String statement, Object[] params){
		OCommandRequest command = genericSQLStatementCommandBuilder(statement);
		Object ret = genericSQLCommandExecute(command,params);
		return ret;
	}
	
	public static void shutdownDB(boolean repopulate){
		ODatabaseRecordTx db = null;

		try{
			//WE GET THE CONNECTION BEFORE SETTING THE SEMAPHORE
			db = getConnection();

			synchronized(DbHelper.class)  {
				if(!dbFreeze.get()){
					dbFreeze.set(true);
				}
				db.drop();
				db.close();
				db.create();
				db.getLevel1Cache().clear();
				db.getLevel2Cache().clear();
				db.reload();
				db.getMetadata().reload();
				if(repopulate){
					HooksManager.registerAll(db);
					setupDb();
				}


			}

		}catch(Throwable e){
			throw new RuntimeException(e);
		}finally{
			synchronized(DbHelper.class)  {

				dbFreeze.set(false);

			}
		}

	}

	public static ODatabaseRecordTx getOrOpenConnection(String appcode, String username,String password) throws InvalidAppCodeException {
		ODatabaseRecordTx db= getConnection();
		if (db==null || db.isClosed()) db = open ( appcode,  username, password) ;
		return db;
	}

	public static ODatabaseRecordTx getOrOpenConnectionWIthHTTPUsername() throws InvalidAppCodeException {
		ODatabaseRecordTx db= getConnection();
		if (db==null || db.isClosed()) db = open (  
				(String) Http.Context.current().args.get("appcode"),  
				getCurrentHTTPUsername(), 
				getCurrentHTTPPassword()) ;
		return db;
	}
	
	public static ODatabaseRecordTx open(String appcode, String username,String password) throws InvalidAppCodeException {
		
		if (appcode==null || !appcode.equals(BBConfiguration.configuration.getString(BBConfiguration.APP_CODE)))
			throw new InvalidAppCodeException("Authentication info not valid or not provided: " + appcode + " is an Invalid App Code");
		if(dbFreeze.get()){
			throw new ShuttingDownDBException();
		}
		String databaseName=BBConfiguration.getDBDir();
		if (Logger.isDebugEnabled()) Logger.debug("opening connection on db: " + databaseName + " for " + username);
		
		ODatabaseDocumentTx conn = new ODatabaseDocumentTx("plocal:" + BBConfiguration.getDBDir());
		conn.open(username,password);
		HooksManager.registerAll(getConnection());
		DbHelper.appcode.set(appcode);
		DbHelper.username.set(username);
		DbHelper.password.set(password);
		
		return getConnection();
	}

    public static boolean isConnectedAsAdmin(boolean excludeInternal){
        OUser user = getConnection().getUser();
        Set<ORole> roles = user.getRoles();
        boolean isAdminRole = roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()));
        return excludeInternal ? isAdminRole && !BBConfiguration.getBaasBoxAdminUsername().equals(user.getName()) : isAdminRole;
    }


	public static ODatabaseRecordTx reconnectAsAdmin (){
		if (tranCount.get()>0) throw new SwitchUserContextException("Cannot switch to admin context within an open transaction");
		getConnection().close();
		try {
			return open (appcode.get(),BBConfiguration.getBaasBoxAdminUsername(),BBConfiguration.getBaasBoxAdminPassword());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}

	public static ODatabaseRecordTx reconnectAsAuthenticatedUser (){
		if (tranCount.get()>0) throw new SwitchUserContextException("Cannot switch to user context within an open transaction");
		getConnection().close();
		try {
			return open (appcode.get(),getCurrentHTTPUsername(),getCurrentHTTPPassword());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void close(ODatabaseRecordTx db) {
		if (Logger.isDebugEnabled()) Logger.debug("closing connection");
		if (db!=null && !db.isClosed()){
			//HooksManager.unregisteredAll(db);
			try{
				if (tranCount.get()!=0) throw new TransactionIsStillOpenException("Closing a connection with an active transaction: " + tranCount.get());
			}finally{
				db.close();
				tranCount.set(0);
			}
		}else if (Logger.isDebugEnabled()) Logger.debug("connection already close or null");
	}

	public static ODatabaseRecordTx getConnection(){
		ODatabaseRecordTx db = null;
		try {
			db=(ODatabaseRecordTx)ODatabaseRecordThreadLocal.INSTANCE.get();
			if (Logger.isDebugEnabled()) Logger.debug("Connection id: " + db + " " + ((Object) db).hashCode());
		}catch (ODatabaseException e){
			Logger.debug("Cound not retrieve the DB connection within this thread: " + e.getMessage());
		}
		return db;
	}

	
	public static String getCurrentHTTPPassword(){
		return (String) Http.Context.current().args.get("password");
	}

	public static String getCurrentHTTPUsername(){
		return (String) Http.Context.current().args.get("username");
	}

	public static String getCurrentUserNameFromConnection(){
		return getConnection().getUser().getName();
	}

	public static boolean isConnectedLikeBaasBox(){
		return getCurrentHTTPUsername().equalsIgnoreCase(BBConfiguration.getBaasBoxUsername());
	}

	public static void createDefaultRoles() throws RoleNotFoundException, RoleAlreadyExistsException{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		RoleService.createInternalRoles();
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	}

	public static void createDefaultUsers() throws Exception{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		UserService.createDefaultUsers();
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	}

	
	public static void updateDefaultUsers() throws Exception{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		ODatabaseRecordTx db = DbHelper.getConnection();
		OUser user=db.getMetadata().getSecurity().getUser(BBConfiguration.getBaasBoxUsername());
		user.setPassword(BBConfiguration.getBaasBoxPassword());
		user.save();

		user=db.getMetadata().getSecurity().getUser(BBConfiguration.getBaasBoxAdminUsername());
		user.setPassword(BBConfiguration.getBaasBoxAdminPassword());
		user.save();

		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	}


	public static void populateDB() throws IOException{
		ODatabaseRecordTx db = getConnection();
		//DO NOT DELETE THE FOLLOWING LINE!
		OrientGraphNoTx dbg =  new OrientGraphNoTx(getODatabaseDocumentTxConnection()); 
		Logger.info("Populating the db...");
		InputStream is;
		if (Play.application().isProd()) is	=Play.application().resourceAsStream(SCRIPT_FILE_NAME);
		else is = new FileInputStream(Play.application().getFile("conf/"+SCRIPT_FILE_NAME));
		List<String> script=IOUtils.readLines(is, "UTF-8");
		is.close();

		for (String line:script){
			if (Logger.isDebugEnabled()) Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Internal.DB_VERSION._setValue(BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION));
		String uniqueId="";
		try{
			UUID u = new UUID();
			uniqueId=new String(Base64.encodeBase64(u.toString().getBytes()));
		}catch (Exception e){
			java.util.UUID u = java.util.UUID.randomUUID();
			uniqueId=new String(Base64.encodeBase64(u.toString().getBytes()));
		}
		Internal.INSTALLATION_ID._setValue(uniqueId);
		Logger.info("Unique installation id is: " + uniqueId);
		Logger.info("...done");
	}

	public static void populateConfiguration () throws IOException, ConfigurationException{
		Logger.info("Load initial configuration...");
		InputStream is;
		if (Play.application().isProd()) is	=Play.application().resourceAsStream(CONFIGURATION_FILE_NAME);
		else is = new FileInputStream(Play.application().getFile("conf/"+CONFIGURATION_FILE_NAME));
		HierarchicalINIConfiguration c = new HierarchicalINIConfiguration();
		c.setEncoding("UTF-8");
		c.load(is);
		CharSequence doubleDot = "..";
		CharSequence dot = ".";

		Set<String> sections= c.getSections();
		for (String section: sections){
			Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
			if (en==null){
				Logger.warn(section  + " is not a valid configuration section, it will be skipped!");
				continue;
			}
			SubnodeConfiguration subConf=c.getSection(section);
			Iterator<String> it = subConf.getKeys();
			while (it.hasNext()){
				String key = (it.next()); 
				Object value =subConf.getString(key);
				key=key.replace(doubleDot, dot);//bug on the Apache library: if the key contain a dot, it will be doubled!
				try {
					Logger.info("Setting "+value+ " to "+key);
					PropertiesConfigurationHelper.setByKey(en, key, value);
				} catch (Exception e) {
					Logger.warn("Error loading initial configuration: Section " + section + ", key: " + key +", value: " + value, e);
				}
			}
		}
		is.close();
		Logger.info("...done");
	}

    static void createDefaultPermissionTags(){
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        PermissionTagService.createDefaultPermissions();
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
    }

	public static void setupDb() throws Exception{
		Logger.info("Creating default roles...");
		DbHelper.createDefaultRoles();
		populateDB();
		getConnection().getMetadata().getIndexManager().reload();
		Logger.info("Creating default users...");
		createDefaultUsers();
		populateConfiguration();
        createDefaultPermissionTags();
	}

	public static void exportData(String appcode,OutputStream os) throws UnableToExportDbException{
		ODatabaseRecordTx db = null;
		try{
			db = open(appcode, BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminPassword());
			
			ODatabaseExport oe = new ODatabaseExport(db, os, new OCommandOutputListener() {
				@Override
				public void onMessage(String m) {
					Logger.info(m);
				}
			});
			synchronized(DbHelper.class)  {
				if(!dbFreeze.get()){
					dbFreeze.set(true);
				}
			}
			oe.setUseLineFeedForRecords(true);
			oe.setIncludeManualIndexes(true);
			oe.exportDatabase();
			oe.close();
		}catch(Exception ioe){
			throw new UnableToExportDbException(ioe);
		}finally{
			if(db!=null && ! db.isClosed()){
				db.close();
			}
			dbFreeze.set(false);
		}
	}
	
	public static void importData(String appcode,String importData) throws UnableToImportDbException{
		ODatabaseRecordTx db = null;
		java.io.File f = null;
		try{
			Logger.info("Initializing restore operation..:");
			Logger.info("...dropping the old db..:");
			DbHelper.shutdownDB(false);
			f = java.io.File.createTempFile("import", ".json");
			FileUtils.writeStringToFile(f, importData);
			synchronized(DbHelper.class)  {
				if(!dbFreeze.get()){
					dbFreeze.set(true);
				}
			}

			db=getConnection(); 
			Logger.info("...unregistering hooks...");
			HooksManager.unregisteredAll(db);
			Logger.info("...drop the O-Classes...");
			db.getMetadata().getSchema().dropClass("OFunction");
			 db.getMetadata().getSchema().dropClass("OSchedule");
			 db.getMetadata().getSchema().dropClass("ORIDs");
			   ODatabaseDocumentTx dbd = new ODatabaseDocumentTx(db);
			ODatabaseImport oi = new ODatabaseImport(dbd, f.getAbsolutePath(), new OCommandOutputListener() {
				@Override
				public void onMessage(String m) {
					Logger.info("Restore db: " + m);
				}
			});
			
			 oi.setIncludeManualIndexes(true);
			 oi.setUseLineFeedForRecords(true);
			 oi.setPreserveClusterIDs(true);
			 oi.setPreserveRids(true);
			 Logger.info("...starting import procedure...");
			 oi.importDatabase();
			 oi.close();
			
			 Logger.info("...setting up internal user credential...");
			 updateDefaultUsers();
			 Logger.info("...setting up DataBase attributes...");
			 setupAttributes();
			 Logger.info("...registering hooks...");
			 evolveDB(db);
			 HooksManager.registerAll(db);
			 Logger.info("...extract iOS certificates...");
			 IosCertificateHandler.init();
		}catch(Exception ioe){
			Logger.error("*** Error importing the db: ", ioe);
			throw new UnableToImportDbException(ioe);
		}finally{
			if(db!=null && ! db.isClosed()){
				db.close();
			}
			Logger.info("...releasing the db...");
			dbFreeze.set(false);
			if(f!=null && f.exists()){
				f.delete();
			}
			Logger.info("...restore terminated");
		}
	}

	private static void setupAttributes() {
		ODatabaseRecordTx db = DbHelper.getConnection();
		DbHelper.execMultiLineCommands(db,Logger.isDebugEnabled(),
				"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.sssZ"
				,"alter database custom useLightweightEdges=false"
				,"alter database custom useClassForEdgeLabel=false"
				,"alter database custom useClassForVertexLabel=true"
				,"alter database custom useVertexFieldsForEdgeLabels=true"
  	        );
	}

	/**
	 * Check the db level and evolve it
	 * @param db
	 */
	public static void evolveDB(ODatabaseRecordTx db) {
		//check for evolutions
		 Logger.info("...looking for evolutions...");
		 String fromVersion="";
		 if (db.getMetadata().getIndexManager().getIndex("_bb_internal")!=null){
			 Logger.info("...db is < 0.7 ....");
			 ORID o = (ORID) db.getMetadata().getIndexManager().getIndex("_bb_internal").get(Internal.DB_VERSION.getKey());
			 ODocument od = db.load(o);
			 fromVersion=od.field("value");
		 }else fromVersion=Internal.DB_VERSION.getValueAsString();
		 Logger.info("...db version is: " + fromVersion);
		 if (!fromVersion.equalsIgnoreCase(BBConfiguration.getApiVersion())){
			 Logger.info("...imported DB needs evolutions!...");
			 Evolutions.performEvolutions(db, fromVersion);
			 Internal.DB_VERSION._setValue(BBConfiguration.getApiVersion());
			 Logger.info("DB version is now " + BBConfiguration.getApiVersion());
		 }//end of evolutions
	}
	


	public static OrientGraph getOrientGraphConnection(){
		return new OrientGraph(getODatabaseDocumentTxConnection(),false);
	}


	public static ODatabaseDocumentTx getODatabaseDocumentTxConnection(){
		return new ODatabaseDocumentTx(getConnection());
	}

    /**
     * Executes a sequence of orient sql commands
     */
    public static void execMultiLineCommands(ODatabaseRecordTx db,boolean log,String ... commands){

    		Logger.debug("Ready to execute these commands: " + commands);
        if (commands==null) return;
        for (String command:commands){
            if (command==null){
                Logger.warn("null command found!! skipping");
                continue;
            }
            if (log)Logger.debug("sql:> "+command);
            if (!command.startsWith("--")&&!command.trim().isEmpty()){
            	if (Logger.isDebugEnabled()) Logger.debug("Executing command: " + command);
                db.command(new OCommandSQL(command.replace(';',' '))).execute();
            }
        }
    }
    
    public static void filterOUserPasswords(boolean activate){
    	HooksManager.enableHidePasswordHook(getConnection(), activate);
    }
    
}
