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
package com.baasbox;

import static play.mvc.Results.badRequest;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import com.baasbox.db.CustomSqlFunctions;
import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.exception.ODatabaseException;



public class Global extends GlobalSettings {
	
	
	  @Override
	  public void beforeStart(Application app) {
		  Logger.info("BaasBox is starting...");
		  Logger.info("Loading plugin...");
	  }
	  
	  @Override
	  public void onStart(Application app) {
	    
	    try{
		    OGlobalConfiguration.TX_LOG_SYNCH.setValue(true);
		    OGlobalConfiguration.TX_COMMIT_SYNCH.setValue(true);
		    final OGraphDatabase db = new OGraphDatabase ( "local:" + BBConfiguration.getDBDir() ) ; 
		    if (!db.exists()) {
		    	Logger.info ("DB does not exist, BaasBox will create a new one");
		    	db.create();
		    	Logger.debug("Creating default roles...");
		    	DbHelper.createDefaultRoles();
		    	Logger.debug("Creating default users...");
		    	DbHelper.dropOrientDefault();
		    	CustomSqlFunctions.registerFunctions();
		    	try {
					DbHelper.populateDB(db);
			    	DbHelper.createDefaultUsers();
			    	DbHelper.populateConfiguration(db);
				} catch (IOException e) {
					Logger.error("!! Error initializing BaasBox!", e);
					Logger.error(ExceptionUtils.getFullStackTrace(e));
					System.exit(-1);
				}
		    	 if (!db.isClosed()){
		    		 db.close();
		    	 }
		    	Logger.info("DB has been create successfully");
		    }
		   
	    }catch (Throwable e){
	    	Logger.error("!! Error initializing BaasBox!", e);
	    	Logger.error("Abnormal BaasBox termination.");
	    	System.exit(1);
	    }
	    Logger.info("BaasBox is Ready.");
	  }  
	  
	  
	  @Override
	  public void onStop(Application app) {
	    Logger.info("BaasBox is shutting down...");
	    try{
		    OGraphDatabase db = DbHelper.getConnection() ;
		    if (!db.isClosed()){
		    	Logger.info("Closing the DB...");
		    	db.close();
		    	Logger.info("...ok");
		    }
	    }catch (ODatabaseException e){
	    	//the db is closed, nothing to do
	    }catch (Throwable e){
	    	Logger.error("!! Error shutting down BaasBox!", e);
	    }
	    Logger.info("BaasBox has stopped");
	  }  
	  

	  public Result onBadRequest(String uri, String error) {
		  Logger.error("Invalid URI: " + uri + " - Error: " + error);
		  if (!Play.isDev())  
			  return badRequest("Don't try to hack the URI!");
		  else return null;
	  }  
	    
	  @Override
	    public Result onHandlerNotFound(RequestHeader request) {
	      if (!Play.isDev())
	    	  return Results.notFound("API not found!");
	      else return null;
	    }
}
