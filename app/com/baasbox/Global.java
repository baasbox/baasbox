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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Play;
import play.api.mvc.EssentialFilter;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.db.DbHelper;
import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.SessionTokenProvider;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.graph.OGraphDatabasePool;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.typesafe.config.Config;

public class Global extends GlobalSettings {
	
	  private static Boolean  justCreated = false;


	@Override
	  public void beforeStart(Application app) {
		  info("BaasBox is starting...");
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
			  OGraphDatabase db = null;
			  try{
				db = new OGraphDatabase ( "plocal:" + config.getString(BBConfiguration.DB_PATH) ) ; 
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

	    OGraphDatabase db =null;
	    try{
	    	if (justCreated){
		    	try {
		    		//we MUST use admin/admin because the db was just created
		    		db = DbHelper.open( BBConfiguration.getAPPCODE(),"admin", "admin");
		    		DbHelper.setupDb(db);
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
	    info("BaasBox is Ready.");
	    String port=Play.application().configuration().getString("http.port");
	    if (port==null) port="9000";
	    String address=Play.application().configuration().getString("http.address");
	    if (address==null) address="localhost";
	    
	    info("");
	    info("To login into the amministration console go to http://" + address +":" + port + "/console");
	    info("Default credentials are: user:admin pass:admin AppCode: 1234567890");
	    info("Documentation is available at http://www.baasbox.com/documentation");
		debug("Global.onStart() ended"); 
	  }
	  
	  
	  
	  @Override
	  public void onStop(Application app) {
		debug("Global.onStop() called");
	    info("BaasBox is shutting down...");
	    try{
	    	info("Closing the DB connections...");
	    	OGraphDatabasePool.global().close();
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
	  
	  
	private ObjectNode prepareError(RequestHeader request, String error) {
		ObjectNode result = Json.newObject();
		ObjectMapper mapper = new ObjectMapper();
			result.put("result", "error");
			result.put("bb_code", "");
			result.put("message", error);
			result.put("resource", request.path());
			result.put("method", request.method());
			result.put("request_header", mapper.valueToTree(request.headers()));
			result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
		return result;
	} 
		
	  @Override
	  public Result onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  return badRequest(result);
		  
	  }  

	// 404
	  @Override
	    public Result onHandlerNotFound(RequestHeader request) {
		  debug("API not found: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, "API not found");
		  result.put("http_code", 404);
		  return notFound(result);
		  
	    }

	  // 500 - internal server error
	  @Override
	  public Result onError(RequestHeader request, Throwable throwable) {
		  error("INTERNAL SERVER ERROR: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, throwable.getMessage());
		  result.put("http_code", 500);
		  result.put("stacktrace", ExceptionUtils.getFullStackTrace(throwable));
		  error(ExceptionUtils.getFullStackTrace(throwable));
		  return internalServerError(result);
		  
	  }


	@Override
	public <T extends EssentialFilter> Class<T>[] filters() {
		
		return new Class[]{com.baasbox.filters.LoggingFilter.class};
	}

	  
	  
	  
	
	    
	  //these are needed to override the standard action calls and to centralized the errors response
	   //TODO: we must implement the Play! 2.1 Filters
	    /*
	   @Override
	  public Action onRequest(Request request, Method actionMethod) {
		  return new ActionWrapper(super.onRequest(request, actionMethod));
	  }
	  
	  private class ActionWrapper extends Action.Simple {
		    public ActionWrapper(Action action) {
		      this.delegate = action;
		    }

			@Override
			public Result call(Context ctx) throws Throwable {
				Http.Context.current.set(ctx);
				//injects the CORS  header 
				ctx.response().setHeader("Access-Control-Allow-Origin", "*");
				//injects the user data & credential into the context
				String token=ctx.request().getHeader(SessionKeys.TOKEN.toString());
				if (token!=null) {
					  ImmutableMap<SessionKeys, ? extends Object> sessionData = SessionTokenProvider.getSessionTokenProvider().getSession(token);
					  if (sessionData!=null){
							ctx.args.put("username", sessionData.get(SessionKeys.USERNAME));
							ctx.args.put("password", sessionData.get(SessionKeys.PASSWORD));
							ctx.args.put("appcode", sessionData.get(SessionKeys.APP_CODE));
					  }
				}
			    
				//executes the request
				Result result = this.delegate.call(ctx);
			    
				//checks the result of the request
			    final int statusCode = JavaResultExtractor.getStatus(result);
			    if (statusCode>399){	//an error occured
				      final byte[] body = JavaResultExtractor.getBody(result);
				      String stringBody = new String(body, "UTF-8");
				      switch (statusCode) {
				      	case 400: return onBadRequest(ctx.request(),stringBody);
				      	case 401: return onUnauthorized(ctx.request(),stringBody);
				      	case 403: return onForbidden(ctx.request(),stringBody);
				      	case 404: return onResourceNotFound(ctx.request(),stringBody);
				      	default:  return onDefaultError(statusCode,ctx.request(),stringBody);
				      }
			    }
			    return result;
			      //play.api.mvc.SimpleResult wrappedResult = (play.api.mvc.SimpleResult) result.getWrappedResult();
			      //Response response = ctx.response();
			}//call
		  }//class ActionWrapper
	  */
	
}
