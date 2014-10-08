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

import static play.Logger.debug;
import static play.Logger.error;
import static play.Logger.info;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;

import com.baasbox.security.ScriptingSandboxSecutrityManager;
import play.libs.F;
import play.mvc.*;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.api.mvc.EssentialFilter;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.db.DbHelper;
import com.baasbox.metrics.BaasBoxMetric;
import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.storage.StatisticsService;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.ODatabaseException;

public class Global extends GlobalSettings {
	static {
        /*Initialize this before anything else to avoid reflection*/
        ScriptingSandboxSecutrityManager.init();
    }

	  private static Boolean  justCreated = false;


	@Override
	  public void beforeStart(Application app) {
		  info("BaasBox is starting...");
		  info("System details:");
		  info(StatisticsService.os().toString());
		  info(StatisticsService.memory().toString());
		  info(StatisticsService.java().toString());
		  if (Boolean.parseBoolean(app.configuration().getString(BBConfiguration.DUMP_DB_CONFIGURATION_ON_STARTUP))) info(StatisticsService.db().toString());
		 
		  info("...Loading plugin...");
	  }
	  
	  @Override
	  public Configuration onLoadConfig(Configuration config,
          java.io.File path,
          java.lang.ClassLoader classloader){  
		  debug("Global.onLoadConfig() called");
		  info("BaasBox is preparing OrientDB Embedded Server...");
		  try{
			  OGlobalConfiguration.TX_LOG_SYNCH.setValue(Boolean.TRUE);
			  OGlobalConfiguration.TX_COMMIT_SYNCH.setValue(Boolean.TRUE);
			  
			  OGlobalConfiguration.NON_TX_RECORD_UPDATE_SYNCH.setValue(Boolean.TRUE);
			  //Deprecated due to OrientDB 1.6
			  //OGlobalConfiguration.NON_TX_CLUSTERS_SYNC_IMMEDIATELY.setValue(OMetadata.CLUSTER_MANUAL_INDEX_NAME);
			  
			  OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(Boolean.FALSE);
			  OGlobalConfiguration.CACHE_LEVEL2_ENABLED.setValue(Boolean.FALSE);
			  
			  OGlobalConfiguration.INDEX_MANUAL_LAZY_UPDATES.setValue(-1);
			  OGlobalConfiguration.FILE_LOCK.setValue(false);
			  
			  OGlobalConfiguration.FILE_DEFRAG_STRATEGY.setValue(1);
			  
			  OGlobalConfiguration.MEMORY_USE_UNSAFE.setValue(false);
			  
			  
			  Orient.instance().startup();
			  ODatabaseDocumentTx db = null;
			  try{
				db =  Orient.instance().getDatabaseFactory().createDatabase("graph", "plocal:" + config.getString(BBConfiguration.DB_PATH) );
				if (!db.exists()) {
					info("DB does not exist, BaasBox will create a new one");
					db.create();
					justCreated  = true;
				}
			  } catch (Throwable e) {
					error("!! Error initializing BaasBox!", e);
					error(ExceptionUtils.getFullStackTrace(e));
					throw e;
			  } finally {
		    	 if (db!=null && !db.isClosed()) db.close();
			  }
			  info("DB has been create successfully");
		    }catch (Throwable e){
		    	error("!! Error initializing BaasBox!", e);
		    	error("Abnormal BaasBox termination.");
		    	System.exit(-1);
		    }
		  debug("Global.onLoadConfig() ended");
		  return config;
	  }
	  
	  @Override
	  public void onStart(Application app) {
		 debug("Global.onStart() called");
	    //Orient.instance().shutdown();

	    ODatabaseRecordTx db =null;
	    try{
	    	if (justCreated){
		    	try {
		    		//we MUST use admin/admin because the db was just created
		    		db = DbHelper.open( BBConfiguration.getAPPCODE(),"admin", "admin");
		    		DbHelper.setupDb();
			    	info("Initializing session manager");
			    	ISessionTokenProvider stp = SessionTokenProvider.getSessionTokenProvider();
			    	stp.setTimeout(com.baasbox.configuration.Application.SESSION_TOKENS_TIMEOUT.getValueAsInteger()*1000);
		    	}catch (Throwable e){
					error("!! Error initializing BaasBox!", e);
					error(ExceptionUtils.getFullStackTrace(e));
					throw e;
		    	} finally {
		    		if (db!=null && !db.isClosed()) db.close();
		    	}
		    	justCreated=false;
	    	}
	    }catch (Throwable e){
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
	    }
    	info("Updating default users passwords...");
    	try {
    		db = DbHelper.open( BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminPassword());
    		DbHelper.evolveDB(db);
			DbHelper.updateDefaultUsers();
			
			String bbid=Internal.INSTALLATION_ID.getValueAsString();
			if (bbid==null) throw new Exception ("Unique id not found! Hint: could the DB be corrupted?");
			info ("BaasBox unique id is " + bbid);
		} catch (Exception e) {
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
		} finally {
    		if (db!=null && !db.isClosed()) db.close();
    	}
    	
    	try{
    		db = DbHelper.open( BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminPassword());
    		IosCertificateHandler.init();
    	}catch (Exception e) {
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
		} finally {
    		if (db!=null && !db.isClosed()) db.close();
    	}
    	info ("...done");
    	
    	overrideSettings();
    	
    	//activate metrics
    	BaasBoxMetric.setExcludeURIStartsWith(com.baasbox.controllers.routes.Root.startMetrics().url());
    	if (BBConfiguration.getComputeMetrics()) BaasBoxMetric.start();
    	//prepare the Welcome Message
	    String port=Play.application().configuration().getString("http.port");
	    if (port==null) port="9000";
	    String address=Play.application().configuration().getString("http.address");
	    if (address==null) address="localhost";
	    
	    //write the Welcome Message
	    info("");
	    info("To login into the administration console go to http://" + address +":" + port + "/console");
	    info("Default credentials are: user:admin pass:admin AppCode: " + BBConfiguration.getAPPCODE());
	    info("Documentation is available at http://www.baasbox.com/documentation");
		debug("Global.onStart() ended");
	    info("BaasBox is Ready.");
	  }

	private void overrideSettings() {
		info ("Override settings...");
    	//takes only the settings that begin with baasbox.settings
    	Configuration bbSettingsToOverride=BBConfiguration.configuration.getConfig("baasbox.settings");
    	//if there is at least one of them
    	if (bbSettingsToOverride!=null) {
    		//takes the part after the "baasbox.settings" of the key names
    		Set<String> keys = bbSettingsToOverride.keys();
    		Iterator<String> keysIt = keys.iterator();
    		//for each setting to override
    		while (keysIt.hasNext()){
    			String key = keysIt.next();
    			//is it a value to override?
    			if (key.endsWith(".value")){
    				//sets the overridden value
    				String value = "";
    				try {
     					value = bbSettingsToOverride.getString(key);
    					key = key.substring(0, key.lastIndexOf(".value"));
						PropertiesConfigurationHelper.override(key,value);
					} catch (Exception e) {
                        error ("Error overriding the setting " + key + " with the value " + value + ": " +e.getMessage());
					}
    			}else if (key.endsWith(".visible")){ //or maybe we have to hide it when a REST API is called
    				//sets the visibility
    				Boolean value;
    				try {
     					value = bbSettingsToOverride.getBoolean(key);
    					key = key.substring(0, key.lastIndexOf(".visible"));
						PropertiesConfigurationHelper.setVisible(key,value);
					} catch (Exception e) {
						error ("Error overriding the visible attribute for setting " + key + ": " +e.getMessage());
					}
    			}else if (key.endsWith(".editable")){ //or maybe we have to 
    				//sets the possibility to edit the value via REST API by the admin
    				Boolean value;
    				try {
     					value = bbSettingsToOverride.getBoolean(key);
    					key = key.substring(0, key.lastIndexOf(".editable"));
						PropertiesConfigurationHelper.setEditable(key,value);
					} catch (Exception e) {
						error ("Error overriding the editable attribute setting " + key + ": " +e.getMessage());
					}
    			}else { 
    				error("The configuration key: " + key + " is invalid. value, visible or editable are missing");
    			}
    			key.subSequence(0, key.lastIndexOf("."));
    		}
    	}else info ("...No setting to override...");
    	info ("...done");
	}
	  
	  
	  
	  @Override
	  public void onStop(Application app) {
		debug("Global.onStop() called");
	    info("BaasBox is shutting down...");
	    try{
	    	info("Closing the DB connections...");
	    	ODatabaseDocumentPool.global().close();
	    	info("Shutting down embedded OrientDB Server");
	    	Orient.instance().shutdown();
	    	info("...ok");
	    }catch (ODatabaseException e){
	    	error("Error closing the DB!",e);
	    }catch (Throwable e){
	    	error("!! Error shutting down BaasBox!", e);
	    }
	    info("Destroying session manager...");
	    SessionTokenProvider.destroySessionTokenProvider();
	    info("...BaasBox has stopped");
		debug("Global.onStop() ended");
	  }  
	  
	private void setCallIdOnResult(RequestHeader request, ObjectNode result) {
		String callId = request.getQueryString("call_id");
		if (!StringUtils.isEmpty(callId)) result.put("call_id",callId);
	}
	
	private ObjectNode prepareError(RequestHeader request, String error) {
		ObjectNode result = Json.newObject();
		ObjectMapper mapper = new ObjectMapper();
			result.put("result", "error");
			result.put("message", error);
			result.put("resource", request.path());
			result.put("method", request.method());
			result.put("request_header", (JsonNode)mapper.valueToTree(request.headers()));
			result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
			setCallIdOnResult(request, result);
		return result;
	} 
		
	  @Override
	  public F.Promise<SimpleResult> onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 400);
		  SimpleResult resultToReturn =  badRequest(result);
		  try {
			if (Logger.isDebugEnabled()) Logger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + result.toString(),"UTF-8");
		  }finally{
			  return F.Promise.pure (resultToReturn);
		  }
	  }  

	// 404
	  @Override
	    public F.Promise<SimpleResult> onHandlerNotFound(RequestHeader request) {
		  debug("API not found: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, "API not found");
		  result.put("http_code", 404);
		  SimpleResult resultToReturn= notFound(result);
		  try {
			  if (Logger.isDebugEnabled()) Logger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(resultToReturn),"UTF-8"));
		  }finally{
			  return F.Promise.pure (resultToReturn);
		  }
	    }

	  // 500 - internal server error
	  @Override
	  public F.Promise<SimpleResult> onError(RequestHeader request, Throwable throwable) {
		  error("INTERNAL SERVER ERROR: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, throwable.getMessage());
		  result.put("http_code", 500);
		  result.put("stacktrace", ExceptionUtils.getFullStackTrace(throwable));
		  error(ExceptionUtils.getFullStackTrace(throwable));
		  SimpleResult resultToReturn= internalServerError(result);
		  try {
			  if (Logger.isDebugEnabled()) Logger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(resultToReturn),"UTF-8"));
		  } finally{
			  return F.Promise.pure (resultToReturn);
		  }
	  }


	@Override 
	public <T extends EssentialFilter> Class<T>[] filters() {
		
		return new Class[]{com.baasbox.filters.LoggingFilter.class};
	}


	  

	
}