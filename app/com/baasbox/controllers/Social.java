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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.scribe.model.Token;

import play.Logger;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.SocialLoginConfiguration;
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.sociallogin.BaasBoxSocialException;
import com.baasbox.service.sociallogin.BaasBoxSocialTokenValidationException;
import com.baasbox.service.sociallogin.SocialLoginService;
import com.baasbox.service.sociallogin.UnsupportedSocialNetworkException;
import com.baasbox.service.sociallogin.UserInfo;
import com.baasbox.service.user.UserService;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Social extends Controller{
	
	private static final String OAUTH_TOKEN="oauth_token";
	private static final String OAUTH_SECRET="oauth_secret";
	

	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result authorizationUrl(String socialNetwork){
		String keyFormat = socialNetwork.toUpperCase()+"_ENABLED";
		Boolean enabled = SocialLoginConfiguration.valueOf(keyFormat).getValueAsBoolean();
		if(enabled==null || enabled == false){
			return badRequest("Social login for "+socialNetwork+" is not enabled");
		}else{
			SocialLoginService sc = SocialLoginService.by(socialNetwork,(String)ctx().args.get("appcode"));
			return ok("{\"url\":\""+sc.getAuthorizationURL(session())+"\"}");
		}
	}

	/**
	 * This method is a common callback for all oauth
	 * providers.It isn't annotated with a Filter because
	 * social networks callback requests couldn't pass the 
	 * auth headers needed by baasbox.
	 * @param socialNetwork
	 * @return
	 */
	public static Result callback(String socialNetwork){
		try{
			SocialLoginService sc = SocialLoginService.by(socialNetwork,(String)ctx().args.get("appcode"));
			Token t = sc.requestAccessToken(request(),session());
			return ok("{\""+OAUTH_TOKEN+"\":\""+t.getToken()+"\",\""+OAUTH_SECRET+"\":\""+t.getSecret()+"\"}");
		}catch (UnsupportedSocialNetworkException e){
			return badRequest(e.getMessage());
		}catch (java.lang.IllegalArgumentException e){
			return badRequest(e.getMessage());
		}
	}

	
	private static Token extractOAuthTokensFromRequest(Request r){
		//issue #217: "oauth_token" parameter should be moved to request body in Social Login APIs
		Http.RequestBody body = request().body();
		JsonNode bodyJson= body.asJson();
		if (Logger.isDebugEnabled()) Logger.debug("signUp bodyJson: " + bodyJson);

		String authToken = null;
		String authSecret = null;
		
		if (bodyJson.has(OAUTH_TOKEN)) authToken = bodyJson.findValuesAsText(OAUTH_TOKEN).get(0);
		if (bodyJson.has(OAUTH_SECRET)) authSecret = bodyJson.findValuesAsText(OAUTH_SECRET).get(0);
			
		//NOTE: to maintain compatibility with previous versions, we leave the option to use QueryStrings
		if (StringUtils.isEmpty(authToken)) authToken = request().getQueryString(OAUTH_TOKEN);		
		if (StringUtils.isEmpty(authSecret))  authSecret = request().getQueryString(OAUTH_SECRET);	
	
		if(StringUtils.isEmpty(authToken) || StringUtils.isEmpty(authSecret)){
			return null;
		}
		return new Token(authToken,authSecret); 
	}
	
	/**
	 * Login the user through socialnetwork specified
	 * 
	 * An oauth_token and oauth_secret provided by oauth steps
	 * are mandatory 
	 * @param socialNetwork the social network name (facebook,google)
	 * @return 200 status code with the X-BB-SESSION token for further calls
	 */
	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result loginWith(String socialNetwork){
		

		String appcode = (String)ctx().args.get("appcode");
		//after this call, db connection is lost!
		SocialLoginService sc = SocialLoginService.by(socialNetwork,appcode);
		Token t =extractOAuthTokensFromRequest(request());
		if(t==null){
			return badRequest(String.format("Both %s and %s should be specified as query parameters or in the json body",OAUTH_TOKEN,OAUTH_SECRET));
		}
		UserInfo result=null;
		try {
			if(sc.validationRequest(t.getToken())){
				result = sc.getUserInfo(t);
			}else{
				return badRequest("Provided token is not valid");
			}
		} catch (BaasBoxSocialException e1) {
			return badRequest(e1.getError());
		}catch (BaasBoxSocialTokenValidationException e2) {
			return badRequest("Unable to validate provided token");
		}
		if (Logger.isDebugEnabled()) Logger.debug("UserInfo received: " + result.toString());
		result.setFrom(socialNetwork);
		result.setToken(t.getToken());
		//Setting token as secret for one-token only social networks
		result.setSecret(t.getSecret()!=null && StringUtils.isNotEmpty(t.getSecret())?t.getSecret():t.getToken());
		UserDao userDao = UserDao.getInstance();
		ODocument existingUser =  null;
		try{
			existingUser = userDao.getBySocialUserId(result);
		}catch(SqlInjectionException sie){
			return internalServerError(sie.getMessage());
		}

		if(existingUser!=null){
			String username = null;
			try {
				username = UserService.getUsernameByProfile(existingUser);
				if(username==null){
					throw new InvalidModelException("username for profile is null");
				}
			} catch (InvalidModelException e) {
				internalServerError("unable to login with "+socialNetwork+" : "+e.getMessage());
			}
			
			String password = generateUserPassword(username, (Date)existingUser.field(UserDao.USER_SIGNUP_DATE));
			
			ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode,username, password);
			response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
			ObjectNode on = Json.newObject();
			if(existingUser!=null){
				on = (ObjectNode)Json.parse( User.prepareResponseToJson(existingUser));
			}
			on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
			return ok(on);
		}else{
			if (Logger.isDebugEnabled()) Logger.debug("User does not exists with tokens...trying to create");
			String username = UUID.randomUUID().toString();
			Date signupDate = new Date();
			try{
				String password = generateUserPassword(username, signupDate);
				JsonNode privateData = null;
				if(result.getAdditionalData()!=null && !result.getAdditionalData().isEmpty()){
					privateData = Json.toJson(result.getAdditionalData());
				}
				UserService.signUp(username, password, signupDate, null, privateData, null, null,true);
				ODocument profile=UserService.getUserProfilebyUsername(username);
				UserService.addSocialLoginTokens(profile,result);
				ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
				response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
				ObjectNode on = Json.newObject();
				if(profile!=null){
					on = (ObjectNode)Json.parse( User.prepareResponseToJson(profile));
				}
				on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));

				return ok(on);
			}catch(Exception uaee){
				return internalServerError(uaee.getMessage());
			}
		}
	}
	
	
	/**
	 * Returns for the current user the linked accounts to external
	 * social providers.
	 * 
	 * 
	 *  @return a json representation of the list of connected social networks
	 *  404 if no social networks are connected 
	 */
	
	@With({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result socialLogins(){
		try {
			ODocument user = UserService.getCurrentUser();
			Map<String,ODocument> logins = user.field(UserDao.ATTRIBUTES_SYSTEM+"."+UserDao.SOCIAL_LOGIN_INFO);
			if(logins==null || logins.isEmpty()){
				return notFound();
			}else{
				List<UserInfo> result = new ArrayList<UserInfo>();
				for (ODocument d : logins.values()) {
					UserInfo i = UserInfo.fromJson(d.toJSON());
					result.add(i);
				}
				return ok(Json.toJson(result));
			}
		}catch(Exception e){
			return internalServerError(e.getMessage());
		}
	}
	
	/**
	 * Unlink given social network from current user.
	 * In case that the user was generated by any social network and
	 * at the moment of the unlink the user has only one social network connected
	 * the controller will throw an Exception with a clear message.
	 * Otherwise a 200 code will be returned
	 * @param socialNetwork
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result unlink(String socialNetwork){
			ODocument user = null;
			try{
				user = UserService.getCurrentUser();
			}catch(Exception e){
				internalServerError(e.getMessage());
			}
			Map<String,ODocument> logins = user.field(UserDao.ATTRIBUTES_SYSTEM+"."+UserDao.SOCIAL_LOGIN_INFO);
			if(logins==null || logins.isEmpty() || !logins.containsKey(socialNetwork) || logins.get(socialNetwork)==null){
				return notFound("User's account is not linked with "+ StringUtils.capitalize(socialNetwork));
			}else{
				boolean generated = (Boolean)user.field(UserDao.ATTRIBUTES_SYSTEM+"."+UserDao.GENERATED_USERNAME);
				if(logins.size()==1 && generated){
					return internalServerError("User's account can't be unlinked.");
				}else{
					try{
						UserService.removeSocialLoginTokens(user,socialNetwork);
						return ok();
					}catch(Exception e){
						return internalServerError(e.getMessage());
					}
				}
			}
	}
	
	/**
	 * links current user with specified social network param
	 * 
	 * In case the token obtained by the service is already existing in the database
	 * for another user an exception is raised
	 * @param socialNetwork the social network to be linked to
	 * @return a 200 code if the link is correctly generated
	 * 
	 *  
	 */
	@With ({UserCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result linkWith(String socialNetwork){
		//issue #217: "oauth_token" parameter should be moved to request body in Social Login APIs
		Token t = extractOAuthTokensFromRequest(request());
		if(t==null){
			return badRequest("Both '"+OAUTH_TOKEN+"' and '"+OAUTH_SECRET+"' should be specified.");
		}

		String appcode = (String)ctx().args.get("appcode");
		SocialLoginService sc = SocialLoginService.by(socialNetwork,appcode);
		
		UserInfo result=null;
		try {
			if(sc.validationRequest(t.getToken())){
				result = sc.getUserInfo(t);
			}else{
				return badRequest("Provided token is not valid.");
			}
		} catch (BaasBoxSocialException e1) {
			return badRequest(e1.getError());
		}
		 catch (BaasBoxSocialTokenValidationException e2) {
				return badRequest("Unable to validate provided token.");
			}
		result.setFrom(socialNetwork);
		result.setToken(t.getToken());
		
		//Setting token as secret for one-token only social networks
		result.setSecret(t.getSecret()!=null && StringUtils.isNotEmpty(t.getSecret())?t.getSecret():t.getToken());
		ODocument user;
		try {
			user = UserService.getCurrentUser();
			ODocument other = UserDao.getInstance().getBySocialUserId(result);
			boolean sameUser = other!=null && other.getIdentity().equals(user.getIdentity());
			if(other==null || !sameUser){
				UserService.addSocialLoginTokens(user, result);
			}else{
				internalServerError("A user with this token already exists and it's not the current user.");
			}
			return ok();
		} catch (SqlInjectionException e) {
			return internalServerError(e.getMessage());
		}
		
		
		
	}
	

	private static String generateUserPassword(String username,Date signupDate){
		String bbid=Internal.INSTALLATION_ID.getValueAsString();
		String password = Crypto.sign(username+new SimpleDateFormat("ddMMyyyyHHmmss").format(signupDate)+bbid);
		return password;
	}


}
