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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.stringtemplate.v4.ST;

import play.Logger;
import play.Play;
import play.api.templates.Html;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.user.UserService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.Util;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.record.impl.ODocument;

//@Api(value = "/user", listingPath = "/api-docs.{format}/user", description = "Operations about users")
public class User extends Controller {
	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.USER);
	}
	
	static String prepareResponseToJsonUserInfo(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.JSON);
	}
	
	static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.USER);
	}

	/*
	  @Path("/{id}")
	  @ApiOperation(value = "Get info about current user", notes = "", httpMethod = "GET")
	  */
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})	
	  public static Result getCurrentUser() throws SqlInjectionException{
		  Logger.trace("Method Start");
		  ODocument profile = UserService.getCurrentUser();
		  String result=prepareResponseToJson(profile);
		  Logger.trace("Method End");
		  return ok(result);
	  }


	  @With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class})
	  @BodyParser.Of(BodyParser.Json.class)
	  public static Result signUp(){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.trace("signUp bodyJson: " + bodyJson);
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
		  
		  //try to signup new user
		  ODocument profile = null;
		  try {
			  profile = UserService.signUp(username, password,null, nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes,false);
		  } catch (UserAlreadyExistsException e){
			  Logger.debug("signUp", e);
			  return badRequest(username + " already exists");
		  } catch (Throwable e){
			  Logger.warn("signUp", e);
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  }
		  Logger.trace("Method End");
		  ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
		  response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
		  
		  ObjectNode on = Json.newObject();
		  on.put("user", Json.parse( prepareResponseToJsonUserInfo(profile)).get("user"));
		  on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
		  return created(on);
	  }
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  @BodyParser.Of(BodyParser.Json.class)
	  public static Result updateProfile(){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.trace("updateProfile bodyJson: " + bodyJson);
		  if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
		  
		  //extract the profile	 fields
		  JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		  JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		  JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		  JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);

		  if (privateAttributes.has("email")) {
			  //check if email address is valid
			  if (!Util.validateEmail((String) privateAttributes.findValuesAsText("email").get(0)))
				  return badRequest("The email address must be valid.");
		  }
		  
		  ODocument profile;
		  try {
			  profile=UserService.updateCurrentProfile(nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
		  } catch (Throwable e){
			  Logger.warn("updateProfile", e);
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  }
		  Logger.trace("Method End");
		  
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
	  	  Logger.trace("Method Start");
	  	  
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
	  	  } catch (Exception e) {
	  		  Logger.warn("resetPasswordStep1", e);
	  		  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
	  		  else return internalServerError(e.getMessage());
	  }
	  	  Logger.trace("Method End");
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
        
		  try{
		  	  tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
		  	  Logger.debug("resetPasswordStep2 - sRandom: " + tokenReceived);
		  	  
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
		  	  ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
		  	  pageTemplate.add("user_name",username);
		  	  pageTemplate.add("error",e.getMessage());
		      pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
			  DbHelper.getConnection().close();
		      return badRequest(Html.apply(pageTemplate.render()));
		  }
		  String tokenStep2 = ResetPwdDao.getInstance().setTokenStep2(username, appCode);
		  
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
	      pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
	      DbHelper.getConnection().close();
	      return ok(Html.apply(pageTemplate.render()));
		  
	  }

      //NOTE: this controller is called via a web form by a browser to reset the user's password
      //Filters to extract username/appcode/atc.. from the headers have no sense in this case
	  public static Result resetPasswordStep3(String base64) {
		  String tokenReceived="";
	  	  String appCode= "";
	  	  String username = "";
	  	  String tokenId= "";
	  	  Map<String, String[]> bodyForm=null;
	  	  try{
			  //loads the received token and extracts data by the hashcode in the url
			  
		  	  tokenReceived = new String(Base64.decodeBase64(base64.getBytes()));
		  	  Logger.debug("resetPasswordStep3 - sRandom: " + tokenReceived);
		  	  
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
		  	  ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
		  	  pageTemplate.add("user_name",username);
		  	  pageTemplate.add("error",e.getMessage());
		      pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
		      DbHelper.getConnection().close();
		      return badRequest(Html.apply(pageTemplate.render()));
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
		      pageTemplate.add("password","password");
		      pageTemplate.add("repeat_password","repeat-password");
		      pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
		      pageTemplate.add("error",errorString);
		      DbHelper.getConnection().close();
			  return badRequest(Html.apply(pageTemplate.render()));
		  }
		  try {
			  UserService.resetUserPasswordFinalStep(username, password);
		  } catch (Throwable e){
			  Logger.warn("changeUserPassword", e);
			  DbHelper.getConnection().close();
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  } 
		  Logger.trace("Method End");
	  	  
		  String ok_message = "Password changed";
		  ST pageTemplate = new ST(PasswordRecovery.PAGE_HTML_FEEDBACK_TEMPLATE.getValueAsString(), '$', '$');
	  	  pageTemplate.add("user_name",username);
	  	  pageTemplate.add("message",ok_message);
	      pageTemplate.add("application_name",com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString());
	      DbHelper.getConnection().close();
		  return ok(Html.apply(pageTemplate.render()));
	  }
	
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  @BodyParser.Of(BodyParser.Json.class)
	  public static Result changePassword(){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.trace("changePassword bodyJson: " + bodyJson);
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

		  UserService.changePasswordCurrentUser(newPassword);
		  Logger.trace("Method End");
		  return ok();
	  }	  
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  public static Result logoutWithDevice(String deviceId) throws SqlInjectionException { 
		  String token=(String) Http.Context.current().args.get("token");
		  if (!StringUtils.isEmpty(token)) {
			  UserService.logout(deviceId);
			  SessionTokenProvider.getSessionTokenProvider().removeSession(token);
		  }		
		  return noContent();
	  }
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  public static Result logoutWithoutDevice() throws SqlInjectionException { 
		  String token=(String) Http.Context.current().args.get("token");
		  if (!StringUtils.isEmpty(token)) SessionTokenProvider.getSessionTokenProvider().removeSession(token);
		  return noContent();
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
	   */
	  @With ({NoUserCredentialWrapFilter.class})
	  @BodyParser.Of(BodyParser.FormUrlEncoded.class)
	  public static Result login() throws SqlInjectionException {
		 Map<String, String[]> body = request().body().asFormUrlEncoded();
		 if (body==null) return badRequest("missing data: is the body x-www-form-urlencoded?");	
		 String username="";
		 String password="";
		 String appcode="";
		 String loginData=null;
		 	 if(body.get("username")==null) return badRequest("The 'username' field is missing");
			 else username=body.get("username")[0];
			 if(body.get("password")==null) return badRequest("The 'password' field is missing");
			 else password=body.get("password")[0];
			 if(body.get("appcode")==null) return badRequest("The 'appcode' field is missing");
			 else appcode=body.get("appcode")[0];
			 Logger.debug("Username " + username);
			 Logger.debug("Password " + password);
			 Logger.debug("Appcode " + appcode);		
			 if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
					 ||
				 username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername())
			 ) return forbidden(username + " cannot login");
			 
			 if (body.get("login_data")!=null)
				 loginData=body.get("login_data")[0];
			 Logger.debug("LoginData" + loginData);

		  /* other useful parameter to receive and to store...*/		  	  
		  //validate user credentials
		  OGraphDatabase db=null;
		  JsonNode user = null;
		  try{
			 db = DbHelper.open(appcode,username, password);
			 user = Json.parse( prepareResponseToJsonUserInfo(UserService.getCurrentUser())).get("user");
			  
			 
			 if (loginData!=null){
				 JsonNode loginInfo=null;
				 try{
					 loginInfo = Json.parse(loginData);
				 }catch(Exception e){
					 Logger.debug ("Error parsong login_data field");
					 Logger.debug (ExceptionUtils.getFullStackTrace(e));
					 return badRequest("login_data field is not a valid json string");
				 }
				 Iterator<Entry<String, JsonNode>> it =loginInfo.getFields();
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
			  Logger.debug("UserLogin: " +  e.getMessage());
			  return unauthorized("user " + username + " unauthorized");
		  } catch (InvalidAppCodeException e) {
			  Logger.debug("UserLogin: " + e.getMessage());
			  return badRequest("user " + username + " unauthorized");
		  }finally{
			  if (db!=null && !db.isClosed()) db.close();
		  }
		  ImmutableMap<SessionKeys, ? extends Object> sessionObject = SessionTokenProvider.getSessionTokenProvider().setSession(appcode, username, password);
		  response().setHeader(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
		  
		  ObjectNode on = Json.newObject();
		  if(user!=null){
			  on.put("user", user);
		  }
		  on.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
		  
		  
		  return ok(on);
	  }
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  public static Result disable(){
		  	try {
				UserService.disableCurrentUser();
			} catch (UserNotFoundException e) {
				return badRequest(e.getMessage());
			}
		  return ok();
	  }

}
