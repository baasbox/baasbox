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
import static play.libs.Json.toJson;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import play.libs.F;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilterAsync;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.logging.PushLogger;
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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Booleans;

public class Push extends Controller {
//todo lot of duplication in exception handling could be replaced by inheriting from a common base exception

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> send(String username) throws Exception  {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();
		JsonNode bodyJson= body.asJson(); //{"message":"Text"}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("send bodyJson: " + bodyJson);
		if (bodyJson==null) {
			return F.Promise.pure(status(CustomHttpCode.JSON_PAYLOAD_NULL.getBbCode(),CustomHttpCode.JSON_PAYLOAD_NULL.getDescription()));
		}
		JsonNode messageNode=bodyJson.findValue("message");
		if (messageNode==null) {
			return F.Promise.pure(status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription()));
		}
		if(!messageNode.isTextual()) {
			return F.Promise.pure(status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription()));
		}

		String message=messageNode.asText();	


		List<String> usernames = new ArrayList<String>();
		usernames.add(username);

		JsonNode pushProfilesNodes=bodyJson.get("profiles");

		List<Integer> pushProfiles = new ArrayList<Integer>();
		if(pushProfilesNodes!=null){
			if(!(pushProfilesNodes.isArray())){
				return F.Promise.pure(status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription()));
			}
			for(JsonNode pushProfileNode : pushProfilesNodes) {
				pushProfiles.add(pushProfileNode.asInt());
			}	
		} else {
			pushProfiles.add(1);
		}

		return F.Promise.promise(DbHelper.withDbFromContext(ctx(), () -> {
			boolean[] withError = new boolean[6];
			PushService ps = new PushService();
			try {
				if (ps.validate(pushProfiles))
					withError = ps.send(message, usernames, pushProfiles, bodyJson, withError);
			} catch (UserNotFoundException e) {
				BaasBoxLogger.error("Username not found " + username, e);
				return notFound("Username not found");
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (PushNotInitializedException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), CustomHttpCode.PUSH_CONFIG_INVALID.getDescription());
			} catch (PushProfileDisabledException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_PROFILE_DISABLED.getBbCode(), CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription());
			} catch (PushProfileInvalidException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
			} catch (UnknownHostException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_HOST_UNREACHABLE.getBbCode(), CustomHttpCode.PUSH_HOST_UNREACHABLE.getDescription());
			} catch (InvalidRequestException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_INVALID_REQUEST.getBbCode(), CustomHttpCode.PUSH_INVALID_REQUEST.getDescription());
			} catch (PushSoundKeyFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription());
			} catch (PushBadgeFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription());
			} catch (PushActionLocalizedKeyFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription());
			} catch (PushLocalizedKeyFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
			} catch (PushLocalizedArgumentsFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
			} catch (PushCollapseKeyFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription());
			} catch (PushTimeToLiveFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription());
			} catch (PushContentAvailableFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription());
			} catch (PushCategoryFormatException e) {
				BaasBoxLogger.error(ExceptionUtils.getMessage(e));
				return status(CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getBbCode(), CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription());
			}
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			for (int i = 0; i < withError.length; i++) {
				if (withError[i])
					return status(CustomHttpCode.PUSH_SENT_WITH_ERROR.getBbCode(), CustomHttpCode.PUSH_SENT_WITH_ERROR.getDescription());
			}
			return ok();
		})/*,HttpExecutionContext.fromThread(ExecutionContexts.global())*/);

	}


	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> sendUsers() throws Exception {
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			boolean verbose=false;
			try{
				if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
				PushLogger pushLogger = PushLogger.getInstance().init();
				if (UserService.isAnAdmin(DbHelper.currentUsername())) {
					pushLogger.enable();
				}else{
					pushLogger.disable();
				}
				if (request().getQueryString("verbose")!=null && request().getQueryString("verbose").equalsIgnoreCase("true")) verbose=true;
				
				Http.RequestBody body = request().body();
				JsonNode bodyJson= body.asJson(); //{"message":"Text"}
				if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("send bodyJson: " + bodyJson);
				if (bodyJson==null) return  status(CustomHttpCode.JSON_PAYLOAD_NULL.getBbCode(),CustomHttpCode.JSON_PAYLOAD_NULL.getDescription());		  
				pushLogger.addMessage("Payload received: %s",bodyJson.toString());
				JsonNode messageNode=bodyJson.findValue("message");
				pushLogger.addMessage("Payload message received: %s", messageNode==null? "null":messageNode.asText());
				if (messageNode==null) return  status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());	  
				if(!messageNode.isTextual()) return  status(CustomHttpCode.PUSH_MESSAGE_INVALID.getBbCode(),CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription());
			
				String message=messageNode.asText();	
			
				JsonNode usernamesNodes=bodyJson.get("users");
				pushLogger.addMessage("users: %s", usernamesNodes==null? "null" : usernamesNodes.toString() );
			
				List<String> usernames = new ArrayList<String>();
			
				if(!(usernamesNodes==null)){
			
					if(!(usernamesNodes.isArray())) return  status(CustomHttpCode.PUSH_USERS_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_USERS_FORMAT_INVALID.getDescription());
			
					for(JsonNode usernamesNode : usernamesNodes) {
						usernames.add(usernamesNode.asText());
					}	
					
					HashSet<String> hs = new HashSet<String>();
					hs.addAll(usernames);
					usernames.clear();
					usernames.addAll(hs);
					pushLogger.addMessage("Users extracted: %s", Joiner.on(",").join(usernames));
				}
				else {
					return  status(CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getBbCode(),CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getDescription());
				}
			
				JsonNode pushProfilesNodes=bodyJson.get("profiles");
				pushLogger.addMessage("profiles: %s", pushProfilesNodes==null? "null" : pushProfilesNodes.toString() );
			
				List<Integer> pushProfiles = new ArrayList<Integer>();
				if(!(pushProfilesNodes==null)){
					if(!(pushProfilesNodes.isArray())) return  status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
					for(JsonNode pushProfileNode : pushProfilesNodes) {
						if(pushProfileNode.isTextual()) return  status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
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
				pushLogger.addMessage("Profiles computed: %s", Joiner.on(",").join(pushProfiles));

				boolean[] withError=new boolean[6];
				PushService ps=new PushService();

				try{
					boolean isValid=(ps.validate(pushProfiles));
					pushLogger.addMessage("Profiles validation: %s", isValid);
					if (isValid) withError=ps.send(message, usernames, pushProfiles, bodyJson, withError);
					pushLogger.addMessage("Service result: %s", Booleans.join(", ", withError));
				}
				catch (UserNotFoundException e) {
					return notFound("Username not found");
				}
				catch (SqlInjectionException e) {
					return badRequest("The supplied name appears invalid (Sql Injection Attack detected)");
				}
				catch (PushNotInitializedException e){
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_CONFIG_INVALID.getBbCode(), CustomHttpCode.PUSH_CONFIG_INVALID.getDescription());
				}
				catch (PushProfileDisabledException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_PROFILE_DISABLED.getBbCode(),CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription());
				}
				catch (PushProfileInvalidException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription());
				}
				catch (PushInvalidApiKeyException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_INVALID_APIKEY.getBbCode(),CustomHttpCode.PUSH_INVALID_APIKEY.getDescription());
				}
				catch (UnknownHostException e){
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_HOST_UNREACHABLE.getBbCode(),CustomHttpCode.PUSH_HOST_UNREACHABLE.getDescription());
				}
				catch (InvalidRequestException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_INVALID_REQUEST.getBbCode(),CustomHttpCode.PUSH_INVALID_REQUEST.getDescription());
				}	
				catch(IOException e){
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return badRequest(ExceptionUtils.getMessage(e));
				}
				catch(PushSoundKeyFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription());
				}
				catch(PushBadgeFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription());
				}
				catch(PushActionLocalizedKeyFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription());
				}
				catch(PushLocalizedKeyFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
				}
				catch(PushLocalizedArgumentsFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription());
				}	
				catch(PushCollapseKeyFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription());
				}
				catch(PushTimeToLiveFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription());
				}
				catch(PushContentAvailableFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription());
				}
				catch(PushCategoryFormatException e) {
					BaasBoxLogger.error(ExceptionUtils.getMessage(e));
					return status(CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getBbCode(),CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription());
				}
				if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
				for(int i=0;i<withError.length;i++) {
					if(withError[i]) return status(CustomHttpCode.PUSH_SENT_WITH_ERROR.getBbCode(),CustomHttpCode.PUSH_SENT_WITH_ERROR.getDescription());
				}
				PushLogger.getInstance().messages();
				if (UserService.isAnAdmin(DbHelper.currentUsername()) && verbose){
					return ok (toJson(PushLogger.getInstance().messages()));
				} else {
					return ok("Push Notification(s) has been sent");
				}
			}finally{
				if (UserService.isAnAdmin(DbHelper.currentUsername()) && verbose){
					return ok (toJson(PushLogger.getInstance().messages()));
				} 
				BaasBoxLogger.debug("Push execution flow:\n{}", PushLogger.getInstance().toString());
			}
		}));
	}




	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> enablePush(String os, String pushToken) {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if(os==null) {
			return F.Promise.pure(badRequest("OS value cannot be null"));
		}
		if(pushToken==null) return F.Promise.pure(badRequest("pushToken value cannot be null"));
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Trying to enable push to OS: "+os+" pushToken: "+ pushToken);

		Map<String, Object> data = ImmutableMap.of("os", os, UserDao.USER_PUSH_TOKEN, pushToken);
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			UserService.registerDevice(data);
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			return ok();
		}));
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> disablePush(String pushToken) throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if(pushToken==null) return F.Promise.pure(badRequest("pushToken value cannot be null"));
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Trying to disable push to pushToken: "+ pushToken); 
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			UserService.unregisterDevice(pushToken);
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			return ok();
		}));

	}


	/*
	 public static Result sendAll(String message) throws PushNotInitializedException {
	}*/
}
