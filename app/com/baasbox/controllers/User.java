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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.stringtemplate.v4.ST;

import play.Play;
import play.api.templates.Html;
import play.libs.F;
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
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilterAsync;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilterAsync;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilterAsync;
import com.baasbox.controllers.helpers.HttpConstants;
import com.baasbox.controllers.helpers.UserOrientChunker;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.EmailAlreadyUsedException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.AlreadyFriendsException;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.PasswordRecoveryException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.exception.UserToFollowNotExistsException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionObject;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.FriendShipService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.BBJson;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.Util;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
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
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> getCurrentUser() throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(), () -> {
			ODocument profile = UserService.getCurrentUser();
			return ok(prepareResponseToJson(profile));
		}));
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> getUser(String username) throws SqlInjectionException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if (ArrayUtils.contains(
				new String[]{ BBConfiguration.getInstance().getBaasBoxAdminUsername() , BBConfiguration.getInstance().getBaasBoxUsername()},
				username)){
			return F.Promise.pure(badRequest(username + " cannot be queried"));
		}

		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
					ODocument profile = UserService.getUserProfilebyUsername(username);
					if (profile==null) return notFound(username + " not found");
					String result=prepareResponseToJson(profile);
					if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
					return ok(result);
				}));
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class,ExtractQueryParameters.class})
	public static F.Promise<Result> getUsers() {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);

		return F.Promise.promise(DbHelper.withDbFromContext(ctx,()->{
      if (BBConfiguration.getInstance().isChunkedEnabled() && request().version().equals(HttpConstants.HttpProtocol.HTTP_1_1)) {
        if (BaasBoxLogger.isDebugEnabled())
          BaasBoxLogger.info("Prepare to sending chunked response..");
        return getUsersChunked();
      }
			List<ODocument> profiles = UserService.getUsers(criteria,true);
			String result = prepareResponseToJson(profiles);
			return ok(result);
		})).recover((t)->{
			 if (t instanceof SqlInjectionException) {
				 return badRequest(ExceptionUtils.getMessage(t) + " -- " + ExceptionUtils.getRootCauseMessage(t));
			 } else {
				 return internalServerError();
			 }
		});
	}

  private static Result getUsersChunked() {
    final Context ctx = Http.Context.current.get();
    QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
    String select = "";
    try {
      DbHelper.openFromContext(ctx);
      select = DbHelper.selectQueryBuilder(UserDao.MODEL_NAME, criteria.justCountTheRecords(), criteria);
    } catch (InvalidAppCodeException e) {
      return internalServerError("invalid app code");
    } finally {
      DbHelper.close(DbHelper.getConnection());
    }

    final String appcode = DbHelper.getCurrentAppCode();
    final String user = DbHelper.getCurrentHTTPUsername();
    final String pass = DbHelper.getCurrentHTTPPassword();

    UserOrientChunker chunks = new UserOrientChunker(
      appcode
      , user
      , pass
      , ctx);
    if (criteria.isPaginationEnabled())
      criteria.enablePaginationMore();
    chunks.setQuery(select);

    return ok(chunks).as("application/json");
  }

  @With({AdminCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> signUp() throws IOException{
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("signUp bodyJson: " + bodyJson);
		if (bodyJson==null) {
			return F.Promise.pure(badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json"));
		}
		//check and validate input
		if (!bodyJson.has("username")) {
			return F.Promise.pure(badRequest("The 'username' field is missing"));
		}
		if (!bodyJson.has("password")) {
			return F.Promise.pure(badRequest("The 'password' field is missing"));
		}
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
				return F.Promise.pure(badRequest("The email address must be valid."));
		}

		if (StringUtils.isEmpty(password)) {
			return F.Promise.pure(status(422, "The password field cannot be empty"));
		}
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
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
				// Return a generic error message if the username is already in use.
				return badRequest("Error signing up");
			} catch (EmailAlreadyUsedException e){
				// Return a generic error message if the email is already in use.
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("signUp", e);
				return badRequest("Error signing up");
			} catch (Throwable e){
				BaasBoxLogger.warn("signUp", e);
				if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
				else return internalServerError(ExceptionUtils.getMessage(e));
			}
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			SessionObject sessionObject = SessionTokenProviderFactory.getSessionTokenProvider()
					.setSession(appcode, username, password);
			response().setHeader(SessionKeys.TOKEN.toString(), sessionObject.getToken());

			String result=prepareResponseToJson(profile);
			ObjectMapper mapper = BBJson.mapper();
			result = result.substring(0,result.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ (String) sessionObject.getToken()+"\"}";
			JsonNode jn = mapper.readTree(result);

			return created(jn);
		}));
	}



	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> changeUserName() throws UserNotFoundException{
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updateuserName bodyJson: " + bodyJson);
		if (bodyJson==null) {
			return F.Promise.pure(badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json"));
		}
		if (bodyJson.get("username")==null || !bodyJson.get("username").isTextual()) {
			return F.Promise.pure(badRequest("'username' field must be a String"));
		}
		String newUsername=bodyJson.get("username").asText();
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			UserService.changeUsername(DbHelper.getCurrentHTTPUsername(),newUsername);
			return ok();
		}));
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> updateProfile(){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("updateProfile bodyJson: " + bodyJson);
		if (bodyJson==null) {
			return F.Promise.pure(badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json"));
		}

		//extract the profile	 fields
		JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);

		if (privateAttributes!=null && privateAttributes.has("email")) {
			//check if email address is valid
			if (!Util.validateEmail((String) privateAttributes.findValuesAsText("email").get(0)))
				return F.Promise.pure(badRequest("The email address must be valid."));
		}


		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			ODocument profile;
			try {
				profile=UserService.updateCurrentProfile(nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
			} catch (Throwable e){
				BaasBoxLogger.warn("updateProfile", e);
				if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
				else return internalServerError(ExceptionUtils.getMessage(e));
			}
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");

			return ok(prepareResponseToJson(profile));
		}));
	}//updateProfile



	@With ({AdminCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> exists(String username){
		return F.Promise.pure(status(NOT_IMPLEMENTED));
	}


	@With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	public static  F.Promise<Result> resetPasswordStep1(String username){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");

		//check and validate input
		if (username == null) {
			return F.Promise.pure(badRequest("The 'username' field is missing in the URL, please check the documentation"));
		}

		return  F.Promise.promise(DbHelper.withDbFromContext(ctx(), () -> {
			ODocument user;

			if (!UserService.exists(username)) {
				return badRequest("Username " + username + " not found!");
			}
			QueryParams criteria = QueryParams.getInstance().where("user.name=?").params(new String[]{username});

			try {
				List<ODocument> users = UserService.getUsers(criteria);
				user = UserService.getUsers(criteria).get(0);

				ODocument attrObj = user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
				if (attrObj == null || attrObj.field("email") == null)
					return badRequest("Cannot reset password, the \"email\" attribute is not defined into the user's private profile");

				// if (UserService.checkResetPwdAlreadyRequested(username)) return badRequest("You have already requested a reset of your password.");

				String appCode = (String) Context.current.get().args.get("appcode");
				UserService.sendResetPwdMail(appCode, user);
			} catch (PasswordRecoveryException e) {
				BaasBoxLogger.warn("resetPasswordStep1", e);
				return badRequest(ExceptionUtils.getMessage(e));
			} catch (Exception e) {
				BaasBoxLogger.warn("resetPasswordStep1", e);
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			return ok();
		}));
	}


	//NOTE: this controller is called via a web link by a mail client to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static F.Promise<Result> resetPasswordStep2(String base64) throws ResetPasswordException {
		//loads the received token and extracts data by the hashcode in the url

		boolean isJSON = base64.endsWith(".json");

		return F.Promise.promise(()->{

			String tokenReceived="";
			String appCode= "";
			String username = "";
			String tokenId= "";
			String adminUser="";
			String adminPassword = "";
			ObjectNode result = Json.newObject();


			try{
			String decBase64;
				//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				decBase64=base64.substring(0, base64.lastIndexOf('.'));
			} else {
				decBase64 = base64;
			}

			tokenReceived = new String(Base64.decodeBase64(decBase64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep2 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) throw new Exception("The reset password code is invalid. Please repeat the reset password procedure");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			adminUser=BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			adminPassword = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid. Please repeat the reset password procedure");
			}

			boolean isTokenValid=ResetPwdDao.getInstance().verifyTokenStep1(decBase64, username);
			if (!isTokenValid) throw new Exception("Reset password procedure is expired! Please repeat the reset password procedure");

		}catch (Exception e){
			if (isJSON)  {
				result.put("status", "KO");
				result.put("user_name",username);
				result.put("error",ExceptionUtils.getMessage(e));
				result.put("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);
			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",ExceptionUtils.getMessage(e));
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

		});
	}

	//NOTE: this controller is called via a web form by a browser to reset the user's password
	//Filters to extract username/appcode/atc.. from the headers have no sense in this case
	public static F.Promise<Result> resetPasswordStep3(String base64) {
		return F.Promise.promise(()->{

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
			String decBase64;
			//if isJSON it's true, in input I have a json. So I need to delete the "extension" .json
			if(isJSON) {
				decBase64=base64.substring(0, base64.lastIndexOf('.'));
			} else {
				decBase64 = base64;
			}
			//loads the received token and extracts data by the hashcode in the url
			tokenReceived = new String(Base64.decodeBase64(decBase64.getBytes()));
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetPasswordStep3 - sRandom: " + tokenReceived);

			//token format should be APP_Code%%%%Username%%%%ResetTokenId
			String[] tokens = tokenReceived.split("%%%%");
			if (tokens.length!=3) return badRequest("The reset password code is invalid.");
			appCode= tokens[0];
			username = tokens [1];
			tokenId= tokens [2];

			String adminUser=BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
			String adminPassword = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);

			try {
				DbHelper.open(appCode, adminUser, adminPassword);
			} catch (InvalidAppCodeException e1) {
				throw new Exception("The code to reset the password seems to be invalid");
			}

			if (!UserService.exists(username))
				throw new Exception("User not found!");

			boolean isTokenValid = ResetPwdDao.getInstance().verifyTokenStep2(decBase64, username);
			if (!isTokenValid)  throw new Exception("Reset Code not found or expired! Please repeat the reset password procedure");

			Http.RequestBody body = request().body();

			bodyForm= body.asFormUrlEncoded(); 
			if (bodyForm==null) throw new Exception("Error getting submitted data. Please repeat the reset password procedure");

		}catch (Exception e){
			if(isJSON) {
				result.put("user_name", username);
				result.put("error", ExceptionUtils.getMessage(e));
				result.put("application_name", com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
				DbHelper.getConnection().close();
				return badRequest(result);

			}
			else {
				ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
				pageTemplate.add("user_name",username);
				pageTemplate.add("error",ExceptionUtils.getMessage(e));
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
			else return internalServerError(ExceptionUtils.getMessage(e));
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

		});

	}


	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	@BodyParser.Of(BodyParser.Json.class)
	public static F.Promise<Result> changePassword(){
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("changePassword bodyJson: " + bodyJson);
		if (bodyJson==null) {
			return F.Promise.pure(badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json"));
		}

		//check and validate input
		if (!bodyJson.has("old")) {
			return F.Promise.pure(badRequest("The 'old' field is missing"));
		}
		if (!bodyJson.has("new")) {
			return F.Promise.pure(badRequest("The 'new' field is missing"));
		}

		String currentPassword = DbHelper.getCurrentHTTPPassword();
		String oldPassword= bodyJson.findValuesAsText("old").get(0);
		String newPassword= bodyJson.findValuesAsText("new").get(0);

		if (!oldPassword.equals(currentPassword)){
			return F.Promise.pure(badRequest("The old password does not match with the current one"));
		}	  
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try {
				UserService.changePasswordCurrentUser(newPassword);
			} catch (OpenTransactionException e) {
				BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
				throw new RuntimeException(e);
			}
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			return ok();
		}));
	}	  


	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> logoutWithDevice(String pushToken) {
		String token=(String) Http.Context.current().args.get("token");
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			if (!StringUtils.isEmpty(token)) {
				UserService.logout(pushToken);
				SessionTokenProviderFactory.getSessionTokenProvider().removeSession(token);
			}
			return ok("pushToken: " + pushToken + " logged out");
		})).recover((t)->{
			if (t instanceof SqlInjectionException) {
				return badRequest();
			} else {
				return internalServerError();
			}
		});
	}


	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> logoutWithoutDevice() {

		String token=(String) Http.Context.current().args.get("token");
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			if (!StringUtils.isEmpty(token)) SessionTokenProviderFactory.getSessionTokenProvider().removeSession(token);
			return ok("user logged out");
		})).recover((t)->{
			if (t instanceof SqlInjectionException){
				return badRequest();
			} else {
				return internalServerError();
			}
		});

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
	@With ({NoUserCredentialWrapFilterAsync.class})
	public static F.Promise<Result> login()/* throws SqlInjectionException, JsonProcessingException, IOException */{
		final String username;
		final String password;
		final String appcode;
		final String loginData;
		
		RequestBody body = request().body();
		//BaasBoxLogger.debug ("Login called. The body is: {}", body);
		if (body==null) return F.Promise.pure(badRequest("missing data: is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE)));
		
		Map<String, String[]> bodyUrlEncoded = body.asFormUrlEncoded();
		if (bodyUrlEncoded!=null){
			if(bodyUrlEncoded.get("username")==null) 
				return F.Promise.pure(badRequest("The 'username' field is missing"));
			else username=bodyUrlEncoded.get("username")[0];
			if(bodyUrlEncoded.get("password")==null) 
				return F.Promise.pure(badRequest("The 'password' field is missing"));
			else password=bodyUrlEncoded.get("password")[0];
			if(bodyUrlEncoded.get("appcode")==null) 
				return F.Promise.pure(badRequest("The 'appcode' field is missing"));
			else appcode=bodyUrlEncoded.get("appcode")[0];
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password <hidden>");
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);		
			if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername())
					) return F.Promise.pure(forbidden(username + " cannot login"));
	
			if (bodyUrlEncoded.get("login_data")!=null)
				loginData=bodyUrlEncoded.get("login_data")[0];
			else loginData=null;
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData" + loginData);
		}else{
			JsonNode bodyJson = body.asJson();

			if (bodyJson==null) 
				return F.Promise.pure(badRequest("missing data : is the body x-www-form-urlencoded or application/json? Detected: " + request().getHeader(CONTENT_TYPE)));
			if(bodyJson.get("username")==null) 
				return F.Promise.pure(badRequest("The 'username' field is missing"));
			else 
				username=bodyJson.get("username").asText();
			
			if(bodyJson.get("password")==null) 
				return F.Promise.pure(badRequest("The 'password' field is missing"));
			else 
				password=bodyJson.get("password").asText();
			
			if(bodyJson.get("appcode")==null) 
				return F.Promise.pure(badRequest("The 'appcode' field is missing"));
			else 
				appcode=bodyJson.get("appcode").asText();
			
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Username " + username);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Password " + password);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Appcode " + appcode);		
			if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername())
					||
					username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername())
					) return F.Promise.pure(forbidden(username + " cannot login"));
	
			if (bodyJson.get("login_data")!=null)
				loginData=bodyJson.get("login_data").asText();
			else loginData=null;
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("LoginData" + loginData);	
		}

		return F.Promise.promise(()->{
			String user;
			//the filter does not inject the appcode. Actually the login endpoint is the only one that does not enforce a check on the appcode presence. 
			//This is not correct, BTW, for the moment we just patch it

			Http.Context.current().args.put("appcode", appcode);
			try (ODatabaseRecordTx db = DbHelper.open(appcode,username,password)){
				user = prepareResponseToJson(UserService.getCurrentUser());
				if (loginData != null) {
					JsonNode loginInfo = null;
					try {
						loginInfo =Json.parse(loginData);
					} catch (Exception e){
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
				SessionObject sessionObject = SessionTokenProviderFactory.getSessionTokenProvider().setSession(appcode, username, password);
				response().setHeader(SessionKeys.TOKEN.toString(), sessionObject.getToken());

				ObjectMapper mapper = BBJson.mapper();
				user = user.substring(0,user.lastIndexOf("}")) + ",\""+SessionKeys.TOKEN.toString()+"\":\""+ (String) sessionObject.getToken()+"\"}";
				JsonNode jn = mapper.readTree(user);
				return ok(jn);
			} catch (OSecurityAccessException e){
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " +  ExceptionUtils.getMessage(e));
				return unauthorized("user " + username + " unauthorized");
			} catch (InvalidAppCodeException e) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("UserLogin: " + ExceptionUtils.getMessage(e));
				return badRequest("user " + username + " unauthorized");
			}
		});
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> disable(){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{

			try {
				UserService.disableCurrentUser();
			} catch (UserNotFoundException e) {
				return badRequest(ExceptionUtils.getMessage(e));
			} catch (OpenTransactionException e) {
				BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
				throw new RuntimeException(e);
			}
			return ok();

		}));
	}

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> follow(String toFollowUsername){

		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
		String currentUsername = DbHelper.currentUsername();

		try{
			UserService.getOUserByUsername(currentUsername);
		}catch(Exception e){
			return internalServerError(ExceptionUtils.getMessage(e)); 
		}
		try {
			ODocument followed = FriendShipService.follow(currentUsername, toFollowUsername);
			return created(prepareResponseToJson(followed));
		} catch (UserToFollowNotExistsException e){
			return notFound(ExceptionUtils.getMessage(e));
		}catch (UserNotFoundException e) {
			return internalServerError(ExceptionUtils.getMessage(e));
		} catch (AlreadyFriendsException e) {
			return badRequest(ExceptionUtils.getMessage(e));
		} catch (SqlInjectionException e) {
			return badRequest("The username " + toFollowUsername + " is not a valid username. HINT: check if it contains invalid character, the server has encountered a possible SQL Injection attack");
		} catch (IllegalArgumentException e){
			return badRequest(ExceptionUtils.getMessage(e));
		}catch (Exception e){
			return internalServerError(ExceptionUtils.getMessage(e));
		}
		}));
	}


	/***
	 * Returns the followers of the current user
	 * @return
	 */
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class,ExtractQueryParameters.class})
	public static F.Promise<Result> followers(boolean justCountThem, String username){
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		return F.Promise.promise(DbHelper.withDbFromContext(ctx,()->{
			String user;
			if (StringUtils.isEmpty(username)) {
				user=DbHelper.currentUsername();
			} else {
				user = username;
			}

      if (BBConfiguration.getInstance().isChunkedEnabled() && request().version().equals(HttpConstants.HttpProtocol.HTTP_1_1) && !justCountThem) {
        if (BaasBoxLogger.isDebugEnabled())
          BaasBoxLogger.info("Prepare to sending chunked response..");
        return getFollowersChunked(user);
      }
			List<ODocument> listOfFollowers=new ArrayList<ODocument>();
			long count=0;
			try {
				if (justCountThem) count = FriendShipService.getCountFriendsOf(user, criteria);
				else listOfFollowers = FriendShipService.getFriendsOf(user, criteria);
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
		}));

	}


  private static Result getFollowersChunked(String username) {
    final Context ctx = Http.Context.current.get();
    QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
    String select = "";
    try {
      DbHelper.openFromContext(ctx);
      select = FriendShipService.getFriendsOfQuery(username, criteria);
    } catch (InvalidAppCodeException e) {
      return internalServerError("invalid app code");
    } finally {
      DbHelper.close(DbHelper.getConnection());
    }

    final String appcode = DbHelper.getCurrentAppCode();
    final String user = DbHelper.getCurrentHTTPUsername();
    final String pass = DbHelper.getCurrentHTTPPassword();

    UserOrientChunker chunks = new UserOrientChunker(
      appcode
      , user
      , pass
      , ctx);
    if (criteria.isPaginationEnabled())
      criteria.enablePaginationMore();
    chunks.setQuery(select);

    return ok(chunks).as("application/json");
  }

  /***
   * Returns the people those the given user is following
   * @param username
   * @return
   */
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class,ExtractQueryParameters.class})
	public static F.Promise<Result> following (String username){
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);

		return F.Promise.promise(DbHelper.withDbFromContext(ctx,()->{
      String user;
			if (StringUtils.isEmpty(username)) {
				user=DbHelper.currentUsername();
			} else {
				user = username;
			}
      if (BBConfiguration.getInstance().isChunkedEnabled() && request().version().equals(HttpConstants.HttpProtocol.HTTP_1_1)) {
        if (BaasBoxLogger.isDebugEnabled())
          BaasBoxLogger.info("Prepare to sending chunked response..");
        return getFollowingChunked(user);
      }
			try {
				List<ODocument> following = FriendShipService.getFollowing(user, criteria);
				return ok(prepareResponseToJson(following));
			} catch (SqlInjectionException e) {
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}

		}));
	}


  private static Result getFollowingChunked(String username) {
    final Context ctx = Http.Context.current.get();
    QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
    String select = "";
    try {
      DbHelper.openFromContext(ctx);
      select = UserDao.getFollowingSelectQuery(username, criteria);
    } catch (InvalidAppCodeException e) {
      return internalServerError("invalid app code");
    } finally {
      DbHelper.close(DbHelper.getConnection());
    }

    final String appcode = DbHelper.getCurrentAppCode();
    final String user = DbHelper.getCurrentHTTPUsername();
    final String pass = DbHelper.getCurrentHTTPPassword();

    UserOrientChunker chunks = new UserOrientChunker(
      appcode
      , user
      , pass
      , ctx);
    if (criteria.isPaginationEnabled())
      criteria.enablePaginationMore();
    chunks.setQuery(select);

    return ok(chunks).as("application/json");
  }

  @With({UserCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> unfollow(String toUnfollowUsername){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{

			String currentUsername = DbHelper.currentUsername();

			try {
				boolean success = FriendShipService.unfollow(currentUsername,toUnfollowUsername);
				if (success){
					return ok();
				} else {
					return notFound("User "+currentUsername+" is not a friend of "+toUnfollowUsername);
				}
			} catch (UserNotFoundException e) {
				return notFound(ExceptionUtils.getMessage(e));
			} catch (Exception e) {
				return internalServerError(e.getMessage());
			}
		}));
	}

}
