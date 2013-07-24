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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.Play;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.AdminCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.NoUserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.UserDao;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.service.user.UserService;
import com.baasbox.util.JSONFormats;
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
	
	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
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


	  @With ({AdminCredentialWrapFilter.class, ConnectToDBFilter.class,})
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
		  
		  //try to signup new user
		  try {
			  UserService.signUp(username, password, nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
		  } catch (Throwable e){
			  Logger.warn("signUp", e);
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  }
		  Logger.trace("Method End");
		  return created();
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
	  

	  
	  public static Result resetPasswordStep1(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result resetPasswordStep2(){
		  return status(NOT_IMPLEMENTED);
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
		  UserService.changePassword(newPassword);
		  Logger.trace("Method End");
		  return ok();
	  }	  
	  
	  @With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  public static Result logout(String deviceId) throws SqlInjectionException { 
		  String token=(String) Http.Context.current().args.get("token");
		  UserService.logout(deviceId);
		  SessionTokenProvider.getSessionTokenProvider().removeSession(token);
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
			 Logger.debug("Appcode" + appcode);		
			 if (body.get("login_data")!=null)
				 loginData=body.get("login_data")[0];
			 Logger.debug("LoginData" + loginData);

		  /* other useful parameter to receive and to store...*/		  	  
		  //validate user credentials
		  OGraphDatabase db=null;
		  try{
			 db = DbHelper.open(appcode,username, password);
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
		  ObjectNode result = Json.newObject();
		  result.put(SessionKeys.TOKEN.toString(), (String) sessionObject.get(SessionKeys.TOKEN));
		  return ok(result);
	  }
	  
	 

}
