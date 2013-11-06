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

import static play.Logger.debug;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Play;
import play.mvc.Http;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.configuration.Internal;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.db.hook.HooksManager;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.RoleAlreadyExistsException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.ShuttingDownDBException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UnableToExportDbException;
import com.baasbox.exception.UnableToImportDbException;
import com.baasbox.service.role.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.eaio.uuid.UUID;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransactionNoTx;


public class DbHelper {

	private static final String SCRIPT_FILE_NAME="db.sql";
	private static final String CONFIGURATION_FILE_NAME="configuration.conf";

	private static ThreadLocal<Boolean> dbFreeze = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {return Boolean.FALSE;};
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

	public static String currentUsername(){
		return username.get();
	}
	
	public static boolean isInTransaction(){
		OGraphDatabase db = getConnection();
		return !(db.getTransaction() instanceof OTransactionNoTx);
	}

	public static void requestTransaction(){
		OGraphDatabase db = getConnection();
		if (!isInTransaction()){
			Logger.trace("Begin transaction");
			//db.begin();
		}
	}

	public static void commitTransaction(){
		OGraphDatabase db = getConnection();
		if (isInTransaction()){
			Logger.trace("Commit transaction");
			//db.commit();
		}
	}

	public static void rollbackTransaction(){
		OGraphDatabase db = getConnection();
		if (isInTransaction()){
			Logger.trace("Rollback transaction");
			//db.rollback();
		}		
	}

	public static String selectQueryBuilder (String from, boolean count, QueryParams criteria){
		String ret;
		if (count) ret = "select count(*) from ";
		else ret = "select from ";
		ret += from;
		if (criteria.getWhere()!=null && !criteria.getWhere().equals("")){
			ret += " where ( " + criteria.getWhere() + " )";
		}
		if (!count && criteria.getOrderBy()!=null && !criteria.getOrderBy().equals("")){
			ret += " order by " + criteria.getOrderBy();
		}
		if (!count && (criteria.getPage()!=null && criteria.getPage()!=-1)){
			ret += " skip " + (criteria.getPage() * criteria.getRecordPerPage()) +
					" limit " + 	criteria.getRecordPerPage();
		}

		Logger.debug("queryBuilder: " + ret);
		return ret;
	}

	/***
	 * Prepares a select statement
	 * @param from the class to query
	 * @param count if true, perform a count instead of to retrieve the records
	 * @param criteria the criteria to apply in the 'where' clause of the select
	 * @return an OCommandRequest object ready to be passed to the {@link #selectCommandExecute(OCommandRequest, String[])} method
	 * @throws SqlInjectionException If the query is not a select statement
	 */
	public static OCommandRequest selectCommandBuilder(String from, boolean count, QueryParams criteria) throws SqlInjectionException{
		OGraphDatabase db =  DbHelper.getConnection();
		OCommandRequest command = db.command(new OSQLSynchQuery<ODocument>(
				selectQueryBuilder(from, count, criteria)
				));
		if (!command.isIdempotent()) throw new SqlInjectionException();
		Logger.debug("commandBuilder: ");
		Logger.debug("  " + criteria.toString());
		Logger.debug("  " + command.toString());
		return command;
	}

	/***
	 * Executes a select eventually passing the parameters 
	 * @param command
	 * @param params positional parameters
	 * @return the List of the record retrieved (the command MUST be a select)
	 */
	public static List<ODocument> selectCommandExecute(OCommandRequest command, String[] params){
		List<ODocument> queryResult = command.execute((Object[])params);
		return queryResult;
	}
	public static Integer sqlCommandExecute(OCommandRequest command, String[] params){
		Integer updateQueryResult = command.execute((Object[])params);
		return updateQueryResult;
	}
	public static List<ODocument> commandExecute(OCommandRequest command, String[] params){
          List<ODocument> queryResult = command.execute((Object[])params);
          return queryResult;
	}
	
	/**
	 * Prepares the command API to execute an arbitrary SQL statement
	 * @param theQuery
	 * @return
	 */
	public static OCommandRequest genericSQLStatementCommandBuilder (String theQuery){
		OGraphDatabase db =  DbHelper.getConnection();
		OCommandRequest command = db.command(new OCommandSQL(theQuery));
		return command;
	}
	
	/***
	 * Executes a generic SQL command statements
	 * @param command the command to execute prepared by {@link #genericSQLStatementCommandBuilder(String)}
	 * @param params The positional parameters to pass to the statement
	 * @return
	 */
	public static Object genericSQLCommandExecute(OCommandRequest command, String[] params){
		Object queryResult = command.execute((Object[])params);
		return queryResult;
	}
	
	/**
	 * Executes an arbitrary sql statement applying the positional parameters
	 * @param statement
	 * @param params
	 * @return
	 */
	public static Object genericSQLStatementExecute(String statement, String[] params){
		OCommandRequest command = genericSQLStatementCommandBuilder(statement);
		Object ret = genericSQLCommandExecute(command,params);
		return ret;
	}
	
	public static void shutdownDB(boolean repopulate){
		OGraphDatabase db = null;

		try{
			//WE GET THE CONNECTION BEFORE SETTING THE SEMAPHORE
			db = DbHelper.open( BBConfiguration.getAPPCODE(),"admin", "admin");

			synchronized(DbHelper.class)  {
				if(!dbFreeze.get()){
					dbFreeze.set(true);
				}

				db.drop();
				db.create();
				if(repopulate){
					HooksManager.registerAll(db);
					setupDb(db);
				}


			}

		}catch(Exception e){
			e.printStackTrace();
		}finally{
			synchronized(DbHelper.class)  {

				dbFreeze.set(false);

			}
		}

	}

	public static OGraphDatabase open(String appcode, String username,String password) throws InvalidAppCodeException {
		
		if (appcode==null || !appcode.equals(BBConfiguration.configuration.getString(BBConfiguration.APP_CODE)))
			throw new InvalidAppCodeException("Authentication info not valid or not provided: " + appcode + " is an Invalid App Code");
		if(dbFreeze.get()){
			throw new ShuttingDownDBException();
		}
		String databaseName=BBConfiguration.getDBDir();
		Logger.debug("opening connection on db: " + databaseName + " for " + username);
		
		//OGraphDatabase db=OGraphDatabasePool.global().acquire("local:" + BBConfiguration.getDBDir(),username,password);
		OGraphDatabase db=new OGraphDatabase("plocal:" + BBConfiguration.getDBDir()).open(username,password);
		HooksManager.registerAll(db);
		
		DbHelper.appcode.set(appcode);
		DbHelper.username.set(username);
		DbHelper.password.set(password);
		
		return db;
	}

	public static OGraphDatabase reconnectAsAdmin (){
		getConnection().close();
		try {
			return open (appcode.get(),BBConfiguration.getBaasBoxAdminUsername(),BBConfiguration.getBaasBoxAdminPassword());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}

	public static OGraphDatabase reconnectAsAuthenticatedUser (){
		getConnection().close();
		try {
			return open (appcode.get(),DbHelper.username.get(),DbHelper.password.get());
		} catch (InvalidAppCodeException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void close(OGraphDatabase db) {
		Logger.debug("closing connection");
		if (db!=null && !db.isClosed()){
			HooksManager.unregisteredAll(db);
			db.close();
		}else Logger.debug("connection already close or null");
	}

	public static OGraphDatabase getConnection(){
		return new OGraphDatabase ((ODatabaseRecordTx)ODatabaseRecordThreadLocal.INSTANCE.get());
	}

	public static String getCurrentHTTPPassword(){
		return (String) Http.Context.current().args.get("password");
	}

	public static String getCurrentHTTPUsername(){
		return (String) Http.Context.current().args.get("username");
	}

	public static String getCurrentUserName(){
		return getConnection().getUser().getName();
	}

	public static boolean isConnectedLikeBaasBox(){
		return getCurrentHTTPUsername().equalsIgnoreCase(BBConfiguration.getBaasBoxUsername());
	}

	public static void createDefaultRoles() throws RoleNotFoundException, RoleAlreadyExistsException{
		Logger.trace("Method Start");
		RoleService.createInternalRoles();
		Logger.trace("Method End");
	}

	public static void createDefaultUsers() throws Exception{
		Logger.trace("Method Start");
		UserService.createDefaultUsers();
		Logger.trace("Method End");
	}

	
	public static void updateDefaultUsers() throws Exception{
		Logger.trace("Method Start");
		OGraphDatabase db = DbHelper.getConnection();
		OUser user=db.getMetadata().getSecurity().getUser(BBConfiguration.getBaasBoxUsername());
		user.setPassword(BBConfiguration.getBaasBoxPassword());
		user.save();

		user=db.getMetadata().getSecurity().getUser(BBConfiguration.getBaasBoxAdminUsername());
		user.setPassword(BBConfiguration.getBaasBoxAdminPassword());
		user.save();

		Logger.trace("Method End");
	}

	@Deprecated
	public static void dropOrientDefault(){
		Logger.trace("Method Start");
		//nothing to do here
		Logger.trace("Method End");
	}

	public static void populateDB(OGraphDatabase db) throws IOException{
		Logger.info("Populating the db...");
		InputStream is;
		if (Play.application().isProd()) is	=Play.application().resourceAsStream(SCRIPT_FILE_NAME);
		else is = new FileInputStream(Play.application().getFile("conf/"+SCRIPT_FILE_NAME));
		List<String> script=IOUtils.readLines(is, "UTF-8");
		is.close();

		for (String line:script){
			Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Internal.DB_VERSION.setValue(BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION));
		String uniqueId="";
		try{
			UUID u = new UUID();
			uniqueId=new String(Base64.encodeBase64(u.toString().getBytes()));
		}catch (Exception e){
			java.util.UUID u = java.util.UUID.randomUUID();
			uniqueId=new String(Base64.encodeBase64(u.toString().getBytes()));
		}
		Internal.INSTALLATION_ID.setValue(uniqueId);
		Logger.info("Unique installation id is: " + uniqueId);
		Logger.info("...done");
	}

	public static void populateConfiguration (OGraphDatabase db) throws IOException, ConfigurationException{
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

	public static void removeConnectionFromPool() {
		OGraphDatabase db = getConnection();
		String dbName=db.getName();
		String dbUser=db.getUser().getName();
		db.close();
		OGraphDatabasePool.global().remove(dbName, dbUser);
	}

	public static void setupDb(OGraphDatabase db) throws Exception{
		debug("Creating default roles...");
		DbHelper.createDefaultRoles();
		debug("Creating default users...");
		DbHelper.dropOrientDefault();
		populateDB(db);
		createDefaultUsers();
		populateConfiguration(db);
	}

	public static void exportData(String appcode,OutputStream os) throws UnableToExportDbException{
		OGraphDatabase db = null;
		try{
			db = open(appcode, "admin", "admin");
			
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
		OGraphDatabase db = null;
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
			 
			ODatabaseImport oi = new ODatabaseImport(db, f.getAbsolutePath(), new OCommandOutputListener() {
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
			 Logger.info("...registering hooks...");
			 HooksManager.registerAll(db);
			 evolveDB(db);
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

	/**
	 * Check the db level and evolve it
	 * @param db
	 */
	public static void evolveDB(OGraphDatabase db) {
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
			 Internal.DB_VERSION.setValue(BBConfiguration.getApiVersion());
			 Logger.info("DB version is now " + BBConfiguration.getApiVersion());
		 }//end of evolutions
	}
	
	
	
}
