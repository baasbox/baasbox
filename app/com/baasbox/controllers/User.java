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
package com.baasbox.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.baasbox.exception.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.ST;

import com.baasbox.service.logging.BaasBoxLogger;
import play.Play;
import play.api.templates.Html;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.RequestBody;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.EmailAlreadyUsedException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.PasswordRecoveryException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.user.FriendShipService;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.type.tree.OMVRBTreeRIDSet;

//@Api(value = "/user", listingPath = "/api-docs.{format}/user", description = "Operations about users")
public class User extends Controller {
	static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.USER);
	}


	static String prepareResponseToJson(List<ODocument> listOfDoc) {
		response().setContentType("application/json");
		try {
			for (ODocument doc : listOfDoc){
				doc.detach();
				if ( doc.field("user") instanceof ODocument) {
					OMVRBTreeRIDSet roles = ((ODocument) doc.field("user")).field("roles");
					if (roles.size()>1){
						Iterator<OIdentifiable> it = roles.iterator();
						while (it.hasNext()){
							if (((ODocument)it.next().getRecord()).field("name").toString().startsWith(FriendShipService.FRIEND_ROLE_NAME)) {
								it.remove();
							}
						}
					}
				}
			}
			return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.USER);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	  @Path("/{id}")
	  @ApiOperation(value = "Get info about current user", notes = "", httpMethod = "GET")
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})	
	public static Result getCurrentUser() throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		ODocument profile = UserService.getCurrentUser();
		String result=prepareResponseToJson(profile);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok(result);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})	
	public static Result getUser(String username) throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if (ArrayUtils.contains(
				new String[]{ BBConfiguration.getBaasBoxAdminUsername() , BBConfiguration.getBaasBoxUsername()},
				username)) return badRequest(username + " cannot be queried");
		ODocument profile = UserService.getUserProfilebyUsername(username);
		if (profile==null) return notFound(username + " not found");
		String result=prepareResponseToJson(profile);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok(result);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})	
	public static Result getUsers() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		List<ODocument> profiles=null;;
		try {
			profiles = UserService.getUsers(criteria,true);
		} catch (SqlInjectionException e) {
			return badRequest(ExceptionUtils.getMessage(e) + " -- " + ExceptionUtils.getRootCauseMessage(e));
		}
		String result=prepareResponseToJson(profiles);
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok(result);
	}

	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result signUp() throws JsonProcessingException, IOException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("signUp bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		//check and validate input
		if (!bodyJson.has("username"))
			return badRequest("The 'username' field is missing");
		if (!bodyJson.has("password"))
			return badRequest("The 'password' field is missing");		

		//extract mandatory fields
		JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
		String username=(String) bodyJson.findValuesAsText("username").get(0);
		String password=(String)  bodyJson.findValuesAsText("password").get(0);
		String appcode = (String)ctx().args.get("appcode");
		if (privateAttributes!=null && privateAttributes.has("email")) {
			//check if email address is valid
			if (!Util.validateEmail((String) privateAttributes.findValuesAsText("email").get(0)))
				return badRequest("The email address must be valid.");
		}
		if (StringUtils.isEmpty(password)) return status(422,"The password field cannot be empty");

		//try to signup new user
		ODocument profile = null;
		try {
			UserService.signUp(username, password,null, nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes,false);
			//due to issue 412, we have to reload the profile
			profile=UserService.getUserProfilebyUsername(username);
		} catch (InvalidJsonException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
			return badRequest("One or more profile sections is not a valid JSON object");
		} catch (UserAlreadyExistsException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
			return badRequest(username + " already exists");
		} catch (EmailAlreadyUsedException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
			return badRequest(username + ": the email provided is already in use");
		} catch (Throwable e){
			BaasBoxLogger.warn("signUp", e);
			if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			else return internalServerError(e.getMessage());
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
		response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));

		String result=prepareResponseToJson(profile);
		ObjectMapper mapper = new ObjectMapper();
		result = result.substring(0,result.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ (String) sessionObject.get(SessionKeys.TOKEN)+"\"}";
		JsonNode jn = mapper.readTree(result);

		return created(jn);
	}



	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result changeUserName() throws UserNotFoundException{
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updateuserName bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		if (bodyJson.get("username")==null || !bodyJson.get("username").isTextual())
			return badRequest("'username' field must be a String");
		String newUsername=bodyJson.get("username").asText();
		try {
			UserService.changeUsername(DbHelper.getCurrentHTTPUsername(),newUsername);
		} catch (OpenTransactionException e) {
			return internalServerError(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("Username not valid");
		}
		return ok();
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result updateProfile(){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updateProfile bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");

		//extract the profile	 fields
		JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);

		if (privateAttributes!=null && privateAttributes.has("email")) {
			//check if email address is valid
			if (!Util.validateEmail((String) privateAttributes.findValuesAsText("email").get(0)))
				return badRequest("The email address must be valid.");
		}

		ODocument profile;
		try {
			profile=UserService.updateCurrentProfile(nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
		} catch (Throwable e){
			BaasBoxLogger.warn("updateProfile", e);
			if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			else return internalServerError(e.getMessage());
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");

		return ok(prepareResponseToJson(profile)); 
	}//updateProfile



	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result exists(String username){
		return status(NOT_IMPLEMENTED);
		/*
		  boolean result = true;//UserService.exists(username);
		  return ok ("{\"response\": \""+result+"\"}");
		 */
	}




	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static Result resetPasswordStep1(String username){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

		//check and validate input
		if (username == null)
			return badRequest("The 'username' field is missing in the URL, please check the documentation");

		if (!UserService.exists(username))
			return badRequest("Username " + username + " not found!");

		QueryParams criteria = QueryParams.getInstance().where("user.name=?").params(new String [] {username});
		ODocument user;

		try {
			List<ODocument> users = UserService.getUsers(criteria);
			user = UserService.getUsers(criteria).get(0);

			ODocument attrObj = user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
			if (attrObj == null || attrObj.field("email") == null)
				return badRequest("Cannot reset password, the \"email\" attribute is not defined into the user's private profile");

			// if (UserService.checkResetPwdAlreadyRequested(username)) return badRequest("You have already requested a reset of your password.");

			String appCode = (String) Http.Context.current.get().args.get("appcode");
			UserService.sendResetPwdMail(appCode,user);
		} catch (PasswordRecoveryException e) {
			BaasBoxLogger.warn("resetPasswordStep1", e);
			return badRequest(e.getMessage());
		} catch (Exception e) {
			BaasBoxLogger.warn("resetPasswordStep1", e);
			return internalServerError(ExceptionUtils.getFullStackTrace(e));
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok();
	}


	//NOTE: this controller is called via a web link by a mail client to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static Result resetPasswordStep2(String base64) throws ResetPasswordException {
		//loads the received token and extracts data by the hashcode in the url
		String tokenReceived="";
		String appCode= "";
		String username = "";
		String tokenId= "";
		String adminUser="";
		String adminPassword = "";
		Boolean isJSON = false;
		ObjectNode result = Json.newObject();

		if(base64.endsWith(".json")) {
			isJSON = true;
		}


		try{
			//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				base64=base64.substring(0, base64.lastIndexOf('.'));
			}
			tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep2 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) throw new Exception("The reset password code is invalid. Please repeat the reset password procedure");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			adminUser=BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			adminPassword = BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid. Please repeat the reset password procedure");
			}

			boolean isTokenValid=ResetPwdDao.getInstance().verifyTokenStep1(base64, username);
			if (!isTokenValid) throw new Exception("Reset password procedure is expired! Please repeat the reset password procedure");

		}catch (Exception e){
			if (isJSON)  {
				result.put("status", "KO");
				result.put("user_name",username);
				result.put("error",e.getMessage());
				result.put("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);
			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",e.getMessage());
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		String tokenStep2 = ResetPwdDao.getInstance().setTokenStep2(username, appCode);

		if(isJSON) {
			result.put("user_name", username);
			result.put("link","/user/password/reset/" + tokenStep2+".json");
			result.put("token",tokenStep2);
			result.put("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(result);
		}
		else {
			ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_TEMPLATE.getValueAsString(), '$', '$');
			pageTemplate.add("form_template", "<form action='/user/password/reset/" + tokenStep2 + "' method='POST' id='reset_pwd_form'>" +
					"<label for='password'>New password</label>"+
					"<input type='password' id='password' name='password' />" +
					"<label for='repeat-password'>Repeat the new password</label>"+
					"<input type='password' id='repeat-password' name='repeat-password' />" +
					"<button type='submit' id='reset_pwd_submit'>Reset the password</button>" +
					"</form>");
			pageTemplate.add("user_name",username);
			pageTemplate.add("link","/user/password/reset/" + tokenStep2);
			pageTemplate.add("password","password");
			pageTemplate.add("repeat_password","repeat-password");
			pageTemplate.add("token",tokenStep2);
			pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(Html.apply(pageTemplate.render()));
		}
	}

	//NOTE: this controller is called via a web form by a browser to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static Result resetPasswordStep3(String base64) {
		String tokenReceived="";
		String appCode= "";
		String username = "";
		String tokenId= "";
		Map<String, String[]> bodyForm=null;
		Boolean isJSON = false;
		ObjectNode result = Json.newObject();

		if(base64.endsWith(".json")) {
			isJSON = true;
		}
		try{
			//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				base64=base64.substring(0, base64.lastIndexOf('.'));
			}
			//loads the received token and extracts data by the hashcode in the url
			tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep3 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) return badRequest("The reset password code is invalid.");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			String adminUser=BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			String adminPassword = BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid");
			}

			if (!UserService.exists(username))
				throw new Exception("User not found!");

			boolean isTokenValid = ResetPwdDao.getInstance().verifyTokenStep2(base64, username);
			if (!isTokenValid)  throw new Exception("Reset Code not found or expired! Please repeat the reset password procedure");

			Http.RequestBody body = request().body();

			bodyForm= body.asFormUrlEncoded(); 
			if (bodyForm==null) throw new Exception("Error getting submitted data. Please repeat the reset password procedure");

		}catch (Exception e){
			if(isJSON) {
				result.put("user_name", username);
				result.put("error", e.getMessage());
				result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);

			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",e.getMessage());
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		//check and validate input
		String errorString="";
		if (bodyForm.get("password").length != 1)
			errorString="The 'new password' field is missing";
		if (bodyForm.get("repeat-password").length != 1)
			errorString="The 'repeat password' field is missing";	

		String password=(String) bodyForm.get("password")[0];
		String repeatPassword=(String)  bodyForm.get("repeat-password")[0];

		if (!password.equals(repeatPassword)){
			errorString="The new \"password\" field and the \"repeat password\" field must be the same.";
		}
		if (!errorString.isEmpty()){
			if(isJSON) {
				result.put("user_name", username);
				result.put("link","/user/password/reset/" + base64+".json");
				result.put("token",base64);
				result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				result.put("error", errorString);
				DbHelper.getConnection().close();
				return badRequest(result);
			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("form_template", "<form action='/user/password/reset/" + base64 + "' method='POST' id='reset_pwd_form'>" +
						"<label for='password'>New password</label>"+
						"<input type='password' id='password' name='password' />" +
						"<label for='repeat-password'>Repeat the new password</label>"+
						"<input type='password' id='repeat-password' name='repeat-password' />" +
						"<button type='submit' id='reset_pwd_submit'>Reset the password</button>" +
						"</form>");
				pageTemplate.add("user_name",username);
				pageTemplate.add("link","/user/password/reset/" + base64);
				pageTemplate.add("token",base64);
				pageTemplate.add("password","password");
				pageTemplate.add("repeat_password","repeat-password");
				pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				pageTemplate.add("error",errorString);
				DbHelper.getConnection().close();
				return badRequest(Html.apply(pageTemplate.render()));
			}
		}
		try {
			UserService.resetUserPasswordFinalStep(username, password);
		} catch (Throwable e){
			BaasBoxLogger.warn("changeUserPassword", e);
			DbHelper.getConnection().close();
			if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			else return internalServerError(e.getMessage());
		} 
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");

		String ok_message = "Password changed";
		if(isJSON) {
			result.put("user_name", username);
			result.put("message", ok_message);
			result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(result);
		}
		else {
			ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
			pageTemplate.add("user_name",username);
			pageTemplate.add("message",ok_message);
			pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			DbHelper.getConnection().close();
			return ok(Html.apply(pageTemplate.render()));
		}
	}


	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static Result changePassword(){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("changePassword bodyJson: " + bodyJson);
		if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");

		//check and validate input
		if (!bodyJson.has("old"))
			return badRequest("The 'old' field is missing");
		if (!bodyJson.has("new"))
			return badRequest("The 'new' field is missing");	

		String currentPassword = DbHelper.getCurrentHTTPPassword();
		String oldPassword=(String) bodyJson.findValuesAsText("old").get(0);
		String newPassword=(String)  bodyJson.findValuesAsText("new").get(0);

		if (!oldPassword.equals(currentPassword)){
			return badRequest("The old password does not match with the current one");
		}	  

		try {
			UserService.changePasswordCurrentUser(newPassword);
		} catch (OpenTransactionException e) {
			BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
			throw new RuntimeException(e);
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return ok();
	}	  

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result logoutWithDevice(String pushToken) throws SqlInjectionException { 
		String token=(String) Http.Context.current().args.get("token");
		if (!StringUtils.isEmpty(token)) {
			UserService.logout(pushToken);
			SessionTokenProvider.getSessionTokenProvider().removeSession(token);
		}		
		return ok("pushToken: " + pushToken + " logged out");
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result logoutWithoutDevice() throws SqlInjectionException { 
		String token=(String) Http.Context.current().args.get("token");
		if (!StringUtils.isEmpty(token)) SessionTokenProvider.getSessionTokenProvider().removeSession(token);
		return ok("user logged out");
	} 

	/***
	 * Login the user.
	 * parameters: 
	 * username
	 * password
	 * appcode: the App Code (API KEY)
	 * login_data: json serialized string containing info related to the device used by the user. In particular, for push notification, must by supplied:
	 * 	deviceId
	 *    os: (android|ios)
	 * @return
	 * @throws SqlInjectionException 
	 * @throws IOException 
	 * @throws JsonProcessingException 
	 */
	@With ({NoUserCredentialWrapFilter.class})
	public static Result login() throws SqlInjectionException, JsonProcessingException, IOException {
		String username="";
		String password="";
		String appcode="";
		String loginData=null;
		
		RequestBody body = request().body();
		//BaasBoxLogger.debug ("Login called. The body is: {}", body);
		if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
		Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
		if (bodyUrlEncoded!=null){
			if(bodyUrlEncoded.get("username")==null) return badRequest("The 'username' field is missing");
			else username=bodyUrlEncoded.get("username")[0];
			if(bodyUrlEncoded.get("password")==null) return badRequest("The 'password' field is missing");
			else password=bodyUrlEncoded.get("password")[0];
			if(bodyUrlEncoded.get("appcode")==null) return badRequest("The 'appcode' field is missing");
			else appcode=bodyUrlEncoded.get("appcode")[0];
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password " + password);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);		
			if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername())
					) return forbidden(username + " cannot login");
	
			if (bodyUrlEncoded.get("login_data")!=null)
				loginData=bodyUrlEncoded.get("login_data")[0];
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData" + loginData);
		}else{
			JsonNode bodyJson = body.asJson();
			if (bodyJson==null) return badRequest("missing data : is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE));
			if(bodyJson.get("username")==null) return badRequest("The 'username' field is missing");
			else username=bodyJson.get("username").asText();
			if(bodyJson.get("password")==null) return badRequest("The 'password' field is missing");
			else password=bodyJson.get("password").asText();
			if(bodyJson.get("appcode")==null) return badRequest("The 'appcode' field is missing");
			else appcode=bodyJson.get("appcode").asText();
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password " + password);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);		
			if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername())
					) return forbidden(username + " cannot login");
	
			if (bodyJson.get("login_data")!=null)
				loginData=bodyJson.get("login_data").asText();
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData" + loginData);	
		}
		/* other useful parameter to receive and to store...*/		  	  
		//validate user credentials
		ODatabaseRecordTx db=null;
		String user = null;
		try{
			db = DbHelper.open(appcode,username, password);
			user =  prepareResponseToJson(UserService.getCurrentUser());


			if (loginData!=null){
				JsonNode loginInfo=null;
				try{
					loginInfo = Json.parse(loginData);
				}catch(Exception e){
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug ("Error parsong login_data field");
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug (ExceptionUtils.getFullStackTrace(e));
					return badRequest("login_data field is not a valid json string");
				}
				Iterator<Entry<String, JsonNode>> it =loginInfo.fields();
				HashMap<String, Object> data = new HashMap<String, Object>();
				while (it.hasNext()){
					Entry<String, JsonNode> element = it.next();
					String key=element.getKey();
					Object value=element.getValue().asText();
					data.put(key,value);
				}
				UserService.registerDevice(data);
			}
		}catch (OSecurityAccessException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " +  e.getMessage());
			return unauthorized("user " + username + " unauthorized");
		} catch (InvalidAppCodeException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " + e.getMessage());
			return badRequest("user " + username + " unauthorized");
		}finally{
			if (db!=null && !db.isClosed()) db.close();
		}
		ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
		response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));

		ObjectMapper mapper = new ObjectMapper();
		user = user.substring(0,user.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ (String) sessionObject.get(SessionKeys.TOKEN)+"\"}";
		JsonNode jn = mapper.readTree(user);

		return ok(jn);
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result disable(){
		try {
			UserService.disableCurrentUser();
		} catch (UserNotFoundException e) {
			return badRequest(e.getMessage());
		} catch (OpenTransactionException e) {
			BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
			throw new RuntimeException(e);
		}
		return ok();
	}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result follow(String toFollowUsername){

		String currentUsername = DbHelper.currentUsername();

		try{
			UserService.getOUserByUsername(currentUsername);
		}catch(Exception e){
			return internalServerError(e.getMessage()); 
		}
		try {
			ODocument followed = FriendShipService.follow(currentUsername, toFollowUsername);
			return created(prepareResponseToJson(followed));
		} catch (UserToFollowNotExistsException e){
			return notFound(e.getMessage());
		}catch (UserNotFoundException e) {
			return internalServerError(e.getMessage());
		} catch (AlreadyFriendsException e) {
			return badRequest(e.getMessage());
		} catch (SqlInjectionException e) {
			return badRequest("The username " + toFollowUsername + " is not a valid username. HINT: check if it contains invalid character, the server has encountered a possible SQL Injection attack");
		} catch (IllegalArgumentException e){
			return badRequest(e.getMessage());
		}catch (Exception e){
			return internalServerError(e.getMessage());
		}

	}


	/***
	 * Returns the followers of the current user
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result followers(boolean justCountThem, String username){
		if (StringUtils.isEmpty(username)) username=DbHelper.currentUsername();
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		List<ODocument> listOfFollowers=new ArrayList<ODocument>();
		long count=0;
		try {
			if (justCountThem) count = FriendShipService.getCountFriendsOf(username, criteria);
			else listOfFollowers = FriendShipService.getFriendsOf(username, criteria);
		} catch (InvalidCriteriaException e) {
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("The parameters you passed are incorrect. HINT: check if the querystring is correctly encoded");
		}
		if (justCountThem) {
			response().setContentType("application/json");
			return ok("{\"count\": "+ count +" }");
		}
		else{
			String ret = prepareResponseToJson(listOfFollowers);
			return ok(ret);
		}
	}


	/***
	 * Returns the people those the given user is following
	 * @param username
	 * @return
	 */
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result following (String username){
		if (StringUtils.isEmpty(username)) username=DbHelper.currentUsername();
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			List<ODocument> following = FriendShipService.getFollowing(username, criteria);
			return ok(prepareResponseToJson(following));
		} catch (SqlInjectionException e) {
			return internalServerError(ExceptionUtils.getFullStackTrace(e));
		}
	}


	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result unfollow(String toUnfollowUsername){
		String currentUsername = DbHelper.currentUsername();

		try {
			boolean success = FriendShipService.unfollow(currentUsername,toUnfollowUsername);
			if (success){
				return ok();
			} else {
				return notFound("User "+currentUsername+" is not a friend of "+toUnfollowUsername);
			}
		} catch (UserNotFoundException e) {
			return notFound(e.getMessage());
		} catch (Exception e) {
			return internalServerError(e.getMessage());
		}
	}

}
