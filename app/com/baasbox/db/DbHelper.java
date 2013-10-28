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
import java.util.ArrayList;
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
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.ShuttingDownDBException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UnableToExportDbException;
import com.baasbox.exception.UnableToImportDbException;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.eaio.uuid.UUID;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
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


	private static final String fetchPlan = "*:?";

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

	public static OCommandRequest selectCommandBuilder(String from, boolean count, QueryParams criteria) throws SqlInjectionException{
		OGraphDatabase db =  DbHelper.getConnection();
		OCommandRequest command = db.command(new OSQLSynchQuery<ODocument>(
				selectQueryBuilder(from, count, criteria)
				).setFetchPlan(fetchPlan.replace("?", criteria.getDepth().toString())));
		if (!command.isIdempotent()) throw new SqlInjectionException();
		Logger.debug("commandBuilder: ");
		Logger.debug("  " + criteria.toString());
		Logger.debug("  " + command.toString());
		return command;
	}

	public static List<ODocument> commandExecute(OCommandRequest command, String[] params){
		List<ODocument> queryResult = command.execute((Object[])params);
		return queryResult;
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
		return db;
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

	public static void createDefaultRoles(){
		Logger.trace("Method Start");
		OGraphDatabase db = DbHelper.getConnection();
		final ORole anonymousUserRole =  db.getMetadata().getSecurity().createRole(DefaultRoles.ANONYMOUS_USER.toString(), ORole.ALLOW_MODES.DENY_ALL_BUT);
		anonymousUserRole.save();
		final ORole registeredUserRole =  db.getMetadata().getSecurity().createRole(DefaultRoles.REGISTERED_USER.toString(), ORole.ALLOW_MODES.DENY_ALL_BUT);
		registeredUserRole.save();

		final ORole backOfficeRole = db.getMetadata().getSecurity().createRole(DefaultRoles.BACKOFFICE_USER.toString(),ORole.ALLOW_MODES.DENY_ALL_BUT);
		

		registeredUserRole.addRule(ODatabaseSecurityResources.DATABASE, ORole.PERMISSION_READ);
		registeredUserRole.addRule(ODatabaseSecurityResources.SCHEMA, ORole.PERMISSION_READ + ORole.PERMISSION_CREATE
				+ ORole.PERMISSION_UPDATE);
		registeredUserRole.addRule(ODatabaseSecurityResources.CLUSTER + "." + OMetadata.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		registeredUserRole.addRule(ODatabaseSecurityResources.CLUSTER + ".orole", ORole.PERMISSION_READ);
		registeredUserRole.addRule(ODatabaseSecurityResources.CLUSTER + ".ouser", ORole.PERMISSION_READ);
		registeredUserRole.addRule(ODatabaseSecurityResources.ALL_CLASSES, ORole.PERMISSION_ALL);
		registeredUserRole.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, ORole.PERMISSION_ALL);
		registeredUserRole.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_ALL);
		registeredUserRole.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_ALL);


		backOfficeRole.addRule(ODatabaseSecurityResources.DATABASE, ORole.PERMISSION_READ);
		backOfficeRole.addRule(ODatabaseSecurityResources.SCHEMA, ORole.PERMISSION_READ + ORole.PERMISSION_CREATE
				+ ORole.PERMISSION_UPDATE);
		backOfficeRole.addRule(ODatabaseSecurityResources.CLUSTER + "." + OMetadata.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		backOfficeRole.addRule(ODatabaseSecurityResources.CLUSTER + ".orole", ORole.PERMISSION_READ);
		backOfficeRole.addRule(ODatabaseSecurityResources.CLUSTER + ".ouser", ORole.PERMISSION_READ);
		backOfficeRole.addRule(ODatabaseSecurityResources.ALL_CLASSES, ORole.PERMISSION_ALL);
		backOfficeRole.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, ORole.PERMISSION_ALL);
		backOfficeRole.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_ALL);
		backOfficeRole.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_ALL);
		backOfficeRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED,ORole.PERMISSION_ALL); //the backoffice users can access and manipulate all records

		anonymousUserRole.addRule(ODatabaseSecurityResources.DATABASE, ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.SCHEMA, ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.CLUSTER + "." + OMetadata.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.CLUSTER + ".orole", ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.CLUSTER + ".ouser", ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.ALL_CLASSES, ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, 7);
		anonymousUserRole.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);
		anonymousUserRole.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_READ);


		anonymousUserRole.save();
		backOfficeRole.save();
		registeredUserRole.save();
		Logger.trace("Method End");
	}

	public static void createDefaultUsers() throws Exception{
		Logger.trace("Method Start");
		//the baasbox default user used to connect to the DB like anonymous user
		String username=BBConfiguration.getBaasBoxUsername();
		String password=BBConfiguration.getBaasBoxPassword();
		UserService.signUp(username, password,DefaultRoles.ANONYMOUS_USER.toString(),null, null,null,null,null,false);

		//the baasbox default user used to act internally as the administrator
		username=BBConfiguration.getBaasBoxAdminUsername();
		password=BBConfiguration.getBaasBoxAdminPassword();
		UserService.signUp(username, password,DefaultRoles.ADMIN.toString(),null, null,null,null,null,false);

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

	public static void dropOrientDefault(){
		Logger.trace("Method Start");
		OGraphDatabase db = DbHelper.getConnection();
		db.getMetadata().getSecurity().dropUser("reader");
		db.getMetadata().getSecurity().dropUser("writer");
		db.getMetadata().getSecurity().dropRole("reader");
		db.getMetadata().getSecurity().dropRole("writer");
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
			DbHelper.shutdownDB(false);
			db = open(appcode, "admin", "admin");
			
			f = java.io.File.createTempFile("import", ".json");
			FileUtils.writeStringToFile(f, importData);
			synchronized(DbHelper.class)  {
				if(!dbFreeze.get()){
					dbFreeze.set(true);
				}
			}
			
			ODatabaseImport oi = new ODatabaseImport(db, f.getAbsolutePath(), new OCommandOutputListener() {
				@Override
				public void onMessage(String m) {
					Logger.info(m);
				}
			});
			
			//This is very important!!!
			for (ORecordHook hook : new ArrayList<ORecordHook>(db.getHooks())) {
				db.unregisterHook(hook);
			 }
			 oi.setIncludeManualIndexes(true);
			 oi.setUseLineFeedForRecords(true);
			 oi.importDatabase();
			 oi.close();
			 HooksManager.registerAll(db);
		}catch(Exception ioe){
			Logger.error(ioe.getMessage());
			throw new UnableToImportDbException(ioe);
		}finally{
			if(db!=null && ! db.isClosed()){
				db.close();
			}
			dbFreeze.set(false);
			if(f!=null && f.exists()){
				f.delete();
			}
		}
	}
	
	
	
}
