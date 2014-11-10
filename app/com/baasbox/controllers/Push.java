/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.controllers;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.PushProfileDisabledException;
import com.baasbox.service.push.PushProfileInvalidException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.push.providers.PushActionLocalizedKeyFormatException;
import com.baasbox.service.push.providers.PushBadgeFormatException;
import com.baasbox.service.push.providers.PushCategoryFormatException;
import com.baasbox.service.push.providers.PushCollapseKeyFormatException;
import com.baasbox.service.push.providers.PushContentAvailableFormatException;
import com.baasbox.service.push.providers.PushInvalidApiKeyException;
import com.baasbox.service.push.providers.PushLocalizedArgumentsFormatException;
import com.baasbox.service.push.providers.PushLocalizedKeyFormatException;
import com.baasbox.service.push.providers.PushSoundKeyFormatException;
import com.baasbox.service.push.providers.PushTimeToLiveFormatException;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gcm.server.InvalidRequestException;

@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
@BodyParser.Of(BodyParser.Json.class)
public class Push extends Controller {
	public static Result send(String username) throws Exception  {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Http.RequestBody body = request().body();
		JsonNode bodyJson= body.asJson(); //{"message":"Text"}
		if (Logger.isTraceEnabled()) Logger.trace("send bodyJson: " + bodyJson);
		if (bodyJson==null) return status(CustomHttpCode.JSON_PAYLOAD_NULL.getBbCode(),CustomHttpCode.JSON_PAYLOAD_NULL.getDescription());		  
		JsonNode messageNode=bodyJson.findValue("message");
		if (messageNode==null) return status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());	  
		if(!messageNode.isTextual()) return status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());

		String message=messageNode.asText();	


		List<String> usernames = new ArrayList<String>();
		usernames.add(username);

		JsonNode pushProfilesNodes=bodyJson.get("profiles");

		List<Integer> pushProfiles = new ArrayList<Integer>();
		if(!(pushProfilesNodes==null)){
			if(!(pushProfilesNodes.isArray())) return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());						
			for(JsonNode pushProfileNode : pushProfilesNodes) {
				pushProfiles.add(pushProfileNode.asInt());
			}	
		}
		else {
			pushProfiles.add(1);
		}
		boolean[] withError=new boolean[6];
		PushService ps=new PushService();
		try{
				if(ps.validate(pushProfiles)) withError=ps.send(message, usernames, pushProfiles, bodyJson, withError);
		}
		catch (UserNotFoundException e) {
			Logger.error("Username not found " + username, e);
			return notFound("Username not found");
		}
		catch (SqlInjectionException e) {
			return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
		}
		catch (PushNotInitializedException e){
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), CustomHttpCode.PUSH_CONFIG_INVALID.getDescription());
		}
		catch (PushProfileDisabledException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_PROFILE_DISABLED.getBbCode(),CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription());
		}
		catch (PushProfileInvalidException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
		}
		catch (UnknownHostException e){
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_HOST_UNREACHABLE.getBbCode(),CustomHttpCode.PUSH_HOST_UNREACHABLE.getDescription());
		}
		catch (InvalidRequestException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_INVALID_REQUEST.getBbCode(),CustomHttpCode.PUSH_INVALID_REQUEST.getDescription());
		}		
		catch(PushSoundKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription());
		}
		catch(PushBadgeFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription());
		}
		catch(PushActionLocalizedKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription());
		}	
		catch(PushLocalizedKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
		}	
		catch(PushLocalizedArgumentsFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
		}
		catch(PushCollapseKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription());
		}
		catch(PushTimeToLiveFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription());
		}
		catch(PushContentAvailableFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription());
		}
		catch(PushCategoryFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription());
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		for(int i=0;i<withError.length;i++) {
			if(withError[i]==true) return status(CustomHttpCode.PUSH_SENT_WITH_ERROR.getBbCode(),CustomHttpCode.PUSH_SENT_WITH_ERROR.getDescription());
		}		
		return ok();
	}

	public static Result sendUsers() throws Exception {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Http.RequestBody body = request().body();
		JsonNode bodyJson= body.asJson(); //{"message":"Text"}
		if (Logger.isTraceEnabled()) Logger.trace("send bodyJson: " + bodyJson);
		if (bodyJson==null) return status(CustomHttpCode.JSON_PAYLOAD_NULL.getBbCode(),CustomHttpCode.JSON_PAYLOAD_NULL.getDescription());		  
		JsonNode messageNode=bodyJson.findValue("message");
		if (messageNode==null) return status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());	  
		if(!messageNode.isTextual()) return status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());

		String message=messageNode.asText();	

		JsonNode usernamesNodes=bodyJson.get("users");


		List<String> usernames = new ArrayList<String>();

		if(!(usernamesNodes==null)){

			if(!(usernamesNodes.isArray())) return status(CustomHttpCode.PUSH_USERS_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_USERS_FORMAT_INVALID.getDescription());

			for(JsonNode usernamesNode : usernamesNodes) {
				usernames.add(usernamesNode.asText());
			}	
			
			HashSet<String> hs = new HashSet<String>();
			hs.addAll(usernames);
			usernames.clear();
			usernames.addAll(hs);
		}
		else {
			return status(CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getBbCode(),CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getDescription());
		}

		JsonNode pushProfilesNodes=bodyJson.get("profiles");

		List<Integer> pushProfiles = new ArrayList<Integer>();
		if(!(pushProfilesNodes==null)){
			if(!(pushProfilesNodes.isArray())) return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
			for(JsonNode pushProfileNode : pushProfilesNodes) {
				if(pushProfileNode.isTextual()) return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
				pushProfiles.add(pushProfileNode.asInt());
			}	
			
			HashSet<Integer> hs = new HashSet<Integer>();
			hs.addAll(pushProfiles);
			pushProfiles.clear();
			pushProfiles.addAll(hs);
			
		}
		else {
			pushProfiles.add(1);
		}
		boolean[] withError=new boolean[6];
		PushService ps=new PushService();
		try{
			if(ps.validate(pushProfiles)) withError=ps.send(message, usernames, pushProfiles, bodyJson, withError);
		}
		catch (UserNotFoundException e) {
			return notFound("Username not found");
		}
		catch (SqlInjectionException e) {
			return badRequest("The supplied name appears invalid (Sql Injection Attack detected)");
		}
		catch (PushNotInitializedException e){
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), CustomHttpCode.PUSH_CONFIG_INVALID.getDescription());
		}
		catch (PushProfileDisabledException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_PROFILE_DISABLED.getBbCode(),CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription());
		}
		catch (PushProfileInvalidException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
		}
		catch (PushInvalidApiKeyException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_INVALID_APIKEY.getBbCode(),CustomHttpCode.PUSH_INVALID_APIKEY.getDescription());
		}
		catch (UnknownHostException e){
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_HOST_UNREACHABLE.getBbCode(),CustomHttpCode.PUSH_HOST_UNREACHABLE.getDescription());
		}
		catch (InvalidRequestException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_INVALID_REQUEST.getBbCode(),CustomHttpCode.PUSH_INVALID_REQUEST.getDescription());
		}	
		catch(IOException e){
			Logger.error(e.getMessage());
			return badRequest(e.getMessage());
		}
		catch(PushSoundKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription());
		}
		catch(PushBadgeFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription());
		}
		catch(PushActionLocalizedKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription());
		}
		catch(PushLocalizedKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
		}	
		catch(PushLocalizedArgumentsFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
		}		
		catch(PushCollapseKeyFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription());
		}
		catch(PushTimeToLiveFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription());
		}
		catch(PushContentAvailableFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription());
		}
		catch(PushCategoryFormatException e) {
			Logger.error(e.getMessage());
			return status(CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription());
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");

		for(int i=0;i<withError.length;i++) {
			if(withError[i]==true) return status(CustomHttpCode.PUSH_SENT_WITH_ERROR.getBbCode(),CustomHttpCode.PUSH_SENT_WITH_ERROR.getDescription());
		}
		return ok();
	}	 





	public static Result enablePush(String os, String pushToken) throws SqlInjectionException{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if(os==null) return badRequest("OS value cannot be null");
		if(pushToken==null) return badRequest("pushToken value cannot be null");
		if (Logger.isDebugEnabled()) Logger.debug("Trying to enable push to OS: "+os+" pushToken: "+ pushToken); 
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("os",os);
		data.put(UserDao.USER_PUSH_TOKEN, pushToken);
		UserService.registerDevice(data);
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return ok();

	}

	public static Result disablePush(String pushToken) throws SqlInjectionException{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if(pushToken==null) return badRequest("pushToken value cannot be null");
		if (Logger.isDebugEnabled()) Logger.debug("Trying to disable push to pushToken: "+ pushToken); 
		UserService.unregisterDevice(pushToken);
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return ok();

	}


	/*
	 public static Result sendAll(String message) throws PushNotInitializedException {
	}*/
}

