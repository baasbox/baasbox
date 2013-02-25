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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import play.Logger;
import play.Play;
import play.mvc.Http;

import com.baasbox.BBConfiguration;
import com.baasbox.dao.DefaultRoles;
import com.baasbox.db.hook.HooksManager;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
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
	private static final String fetchPlan = "*:?";

	public static boolean isInTransaction(){
		OGraphDatabase db = getConnection();
		return !(db.getTransaction() instanceof OTransactionNoTx);
	}
	
	public static void requestTransaction(){
		OGraphDatabase db = getConnection();
		if (!isInTransaction()){
			Logger.trace("Begin transaction");
			db.begin();
		}
	}
	
	public static void commitTransaction(){
		OGraphDatabase db = getConnection();
		if (isInTransaction()){
			Logger.trace("Commit transaction");
			db.commit();
		}
	}
	
	public static void rollbackTransaction(){
		OGraphDatabase db = getConnection();
		if (isInTransaction()){
			Logger.trace("Rollback transaction");
			db.rollback();
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

	public static OGraphDatabase open(String username,String password){
		OGraphDatabase db=new OGraphDatabase("local:" + BBConfiguration.getDBDir()).open(username, password);
		HooksManager.registerAll(db);
		return db;
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
		backOfficeRole.save();
		
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
		  anonymousUserRole.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, ORole.PERMISSION_READ);
		  anonymousUserRole.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);
		  anonymousUserRole.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_READ);
	      
		anonymousUserRole.save();
		registeredUserRole.save();
		Logger.trace("Method End");
	}
	
	public static void createDefaultUsers() throws Exception{
		Logger.trace("Method Start");
		//the baasbox default user used to connect to the DB like anonymous user
		String username=BBConfiguration.getBaasBoxUsername();
		String password=BBConfiguration.getBaasBoxPassword();
		UserService.signUp(username, password,DefaultRoles.REGISTERED_USER.toString(), null,null,null,null);
		OGraphDatabase db = DbHelper.getConnection();
		OUser admin=db.getMetadata().getSecurity().getUser("admin");
		admin.setPassword(BBConfiguration.configuration.getString(BBConfiguration.ADMIN_PASSWORD));
		admin.save();
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
		
		for (String line:script){
			Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Logger.info("...done");
	}


}
