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
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.node.ObjectNode;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

import com.baasbox.db.CustomSqlFunctions;
import com.baasbox.db.DbHelper;
import com.baasbox.security.SessionTokenProvider;
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
	    	Logger.info("Initilizing session manager");
	    	SessionTokenProvider.initialize();
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
	    Logger.info("Destroying session manager");
	    SessionTokenProvider.destroySessionTokenProvider();
	    Logger.info("BaasBox has stopped");
	  }  
	  
	  
	private ObjectNode prepareError(RequestHeader request, String error) {
		ObjectNode result = Json.newObject();
		result.put("result", "error");
		  result.put("bb_code", "");
		  result.put("message", error);
		  result.put("resource", request.path());
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
		  Logger.debug("API not found: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, "API not found");
		  result.put("http_code", 404);
		  return notFound(result);
	    }

	  // 500 - internal server error
	  @Override
	  public Result onError(RequestHeader request, Throwable throwable) {
		  Logger.error("INTERNAL SERVER ERROR: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, throwable.getMessage());
		  result.put("http_code", 500);
		  result.put("stacktrace", ExceptionUtils.getFullStackTrace(throwable));
		  Logger.error(ExceptionUtils.getFullStackTrace(throwable));
		  return internalServerError(result);
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
