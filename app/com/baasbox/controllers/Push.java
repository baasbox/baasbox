package com.baasbox.controllers;

import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.WrapResponse;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.push.PushService;
import com.baasbox.service.push.providers.PushNotInitializedException;
import com.baasbox.service.user.UserService;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;

@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
@BodyParser.Of(BodyParser.Json.class)
public class Push extends Controller {
	 public static Result send(String username) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException,InvalidRequestException {
		 Logger.trace("Method Start");
		 Http.RequestBody body = request().body();
		 JsonNode bodyJson= body.asJson(); //{"message":"Text"}
		 Logger.trace("send bodyJson: " + bodyJson);
		 if (bodyJson==null) return badRequest("The body payload cannot be empty.");		  
		 JsonNode messageNode=bodyJson.findValue("message");
		 if (messageNode==null) return badRequest("The body payload doesn't contain key message");
		 String message=messageNode.asText();	  
		 PushService ps=new PushService();
		 try{
		    	ps.send(message, username);
		 }
		 catch (UserNotFoundException e) {
			    Logger.error("Username not found " + username, e);
			    return notFound("Username not found");
		 }
		 catch (SqlInjectionException e) {
			    return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
		 }
		 catch (InvalidRequestException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(),e.getMessage());
		 }
		 catch (PushNotInitializedException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), e.getMessage());
		 }
		 catch (UnknownHostException e){
			 	Logger.error(e.getMessage());
			 	return status(CustomHttpCode.PUSH_HOST_UNRECHEABLE.getBbCode(),e.getMessage());
		 }
		 
		
		 Logger.trace("Method End");
		 return ok();
	  }
	
	 public static Result enablePush(String os, String deviceId) throws SqlInjectionException{
		 Logger.trace("Method Start");
		 if(os==null) return badRequest("Os value doesn't not null");
		 if(deviceId==null) return badRequest("DeviceId value doesn't not null");
		 Logger.debug("Trying to enable push to os: "+os+" deviceId: "+ deviceId); 
		 HashMap<String, Object> data = new HashMap<String, Object>();
         data.put("os",os);
         data.put("deviceId", deviceId);
		 UserService.registerDevice(data);
		 Logger.trace("Method End");
		 return ok();
		 
	  }
	   
	 
	/*
	 public static Result sendAll(String message) throws PushNotInitializedException {
	}*/
}

