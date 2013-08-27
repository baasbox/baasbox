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

import static play.libs.Json.toJson;

import java.io.IOException;
import java.net.ConnectException;
import java.security.InvalidParameterException;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.With;

import ch.qos.logback.classic.db.DBHelper;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.controllers.actions.filters.CheckAdminRoleFilter;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.CollectionAlreadyExistsException;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.ConfigurationException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.StatisticsService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.Util;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;

@With  ({UserCredentialWrapFilter.class,ConnectToDBFilter.class, CheckAdminRoleFilter.class,ExtractQueryParameters.class})
public class Admin extends Controller {


	  public static Result getUsers(){
		  Logger.trace("Method Start");
		  Context ctx=Http.Context.current.get();
		  QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		  List<ODocument> users=null;
		  String ret="{[]}";
		  try{
			  users = com.baasbox.service.user.UserService.getUsers(criteria);
		  }catch (SqlInjectionException e ){
			  return badRequest("The request is malformed: check your query criteria");
		  }
		  try{
			  ret=OJSONWriter.listToJSON(users,JSONFormats.Formats.USER.toString());
		  }catch (Throwable e){
			  return internalServerError(ExceptionUtils.getFullStackTrace(e));
		  }
		  Logger.trace("Method End");
		  response().setContentType("application/json");
		  return ok(ret);
	  }

	  public static Result getCollections(){
		  	Logger.trace("Method Start");

			 List<ODocument> result;
			 String ret="{[]}";
			try {
				Context ctx=Http.Context.current.get();
				QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
				result = CollectionService.getCollections(criteria);
			} catch (Exception e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(e.getMessage());
			}
			
			  try{
				  ret=JSONFormats.prepareResponseToJson(result,JSONFormats.Formats.DOCUMENT);
			  }catch (IOException e){
				  return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  }
			  
			 Logger.trace("Method End");
			 response().setContentType("application/json");
			 return ok(ret);
	  }
	  
	  public static Result createCollection(String name) throws Throwable{
		  Logger.trace("Method Start");
		  try{
			  CollectionService.create(name);
		  }catch (CollectionAlreadyExistsException e) {
			 return badRequest(e.getMessage()); 
		  }catch (InvalidCollectionException e) {
			 return badRequest("The collection name " + name + " is invalid");
		  }catch (InvalidModelException e){
			  return badRequest(e.getMessage());
		  }catch (Throwable e){
			  Logger.error(ExceptionUtils.getFullStackTrace(e));
			  throw e;
		  }
		  Logger.trace("Method End");
		  return created();
	  }
	  
	  public static Result getDBStatistics(){
		  OGraphDatabase db = DbHelper.getConnection();
		  ImmutableMap response;
		try {
			response = ImmutableMap.<String,Object>builder().
					put("installation", (Object)ImmutableMap.of(
							"bb_id",Internal.INSTALLATION_ID.getValueAsString()
							,"bb_version", Internal.DB_VERSION.getValueAsString()
							))
					.put("db", StatisticsService.db())
					.put("data",StatisticsService.data())
					.put("os",StatisticsService.os())
					.put("java",StatisticsService.java())
					.put("memory",StatisticsService.memory()).build();
					
		} catch (SqlInjectionException e) {
			Logger.error (ExceptionUtils.getFullStackTrace(e));
			return internalServerError(e.getMessage());
		} catch (InvalidCollectionException e) {
			Logger.error (ExceptionUtils.getFullStackTrace(e));
			return internalServerError(e.getMessage());
		}
		response().setContentType("application/json");
		return ok(toJson(response));
	  }
	  
	  public static Result createRole(){
		  return status(NOT_IMPLEMENTED);
	  }
	  

	  public static Result getRoles() throws SqlInjectionException{
		  List<ODocument> listOfRoles=UserService.getRoles();
		  String ret = OJSONWriter.listToJSON(listOfRoles, JSONFormats.Formats.ROLES.toString());
		  response().setContentType("application/json");
		  return ok(ret);
	  }
	  
	  /* create user in any role */
	  
	  public static Result createUser(){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.debug("signUp bodyJson: " + bodyJson);
	
		  //check and validate input
		  if (!bodyJson.has("username"))
			  return badRequest("The 'username' field is missing");
		  if (!bodyJson.has("password"))
			  return badRequest("The 'password' field is missing");		
		  if (!bodyJson.has("role"))
			  return badRequest("The 'role' field is missing");	
		  
		  //extract fields
		  JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		  JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		  JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		  JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
		  String username=(String) bodyJson.findValuesAsText("username").get(0);
		  String password=(String)  bodyJson.findValuesAsText("password").get(0);
		  String role=(String)  bodyJson.findValuesAsText("role").get(0);
		  
		  if (privateAttributes.has("email")) {
			  //check if email address is valid
			  if (!Util.validateEmail((String) (String) privateAttributes.findValuesAsText("email").get(0)) )
				  return badRequest("The email address must be valid.");
		  }
		  
		  //try to signup new user
		  try {
			  UserService.signUp(username, password, role,nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
		  }catch(InvalidParameterException e){
			  return badRequest(e.getMessage());  
		  }catch (OSerializationException e){
			  return badRequest("Body is not a valid JSON: " + e.getMessage() + "\nyou sent:\n" + bodyJson.toString() + 
					  			"\nHint: check the fields "+UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER+
					  			", " + UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER+
					  			", " + UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER  + 
					  			", " + UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER+
					  			" they must be an object, not a value.");
		  }catch (Throwable e){
			  Logger.warn("signUp", e);
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  }
		  Logger.trace("Method End");
		  return created();
	  }//createUser
	  
	  
	  public static Result updateUser(String username){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.debug("signUp bodyJson: " + bodyJson);
	
		  if (!bodyJson.has("role"))
			  return badRequest("The 'role' field is missing");	
		  
		  //extract fields
		  JsonNode nonAppUserAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
		  JsonNode privateAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
		  JsonNode friendsAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
		  JsonNode appUsersAttributes = bodyJson.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
		  String role=(String)  bodyJson.findValuesAsText("role").get(0);
		  
		  if (privateAttributes.has("email")) {
			  //check if email address is valid
			  if (!Util.validateEmail((String) (String) privateAttributes.findValuesAsText("email").get(0)) )
				  return badRequest("The email address must be valid.");
		  }
		  
		  //try to update new user
		  try {
			  UserService.updateProfile(username,role,nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
		  }catch(InvalidParameterException e){
			  return badRequest(e.getMessage());  
		  }catch (OSerializationException e){
			  return badRequest("Body is not a valid JSON: " + e.getMessage() + "\nyou sent:\n" + bodyJson.toString() + 
					  			"\nHint: check the fields "+UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER+
					  			", " + UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER+
					  			", " + UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER  + 
					  			", " + UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER+
					  			" they must be an object, not a value.");
		  }catch (Throwable e){
			  Logger.warn("signUp", e);
			  if (Play.isDev()) return internalServerError(ExceptionUtils.getFullStackTrace(e));
			  else return internalServerError(e.getMessage());
		  }
		  Logger.trace("Method End");
		  return ok();
	  }//updateUser

	  
	  public static Result dropUser(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result dropCollection(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result dropRole(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result deleteDocument(){
		  return status(NOT_IMPLEMENTED);
	  }

	  public static Result dumpConfiguration(String returnType){
		  String dump="";
		  if (returnType.equals("txt")) {
			 dump= PropertiesConfigurationHelper.dumpConfiguration();
			 response().setContentType("application/text");
		 }else if (returnType.equals("json")){
			 dump = PropertiesConfigurationHelper.dumpConfigurationAsJson();
			 response().setContentType("application/json");
		 }
		 return ok(dump);
	  }
	  
	  
	   public static Result setConfiguration(String section, String subSection, String key, String value){
		  Class conf = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
		  if (conf==null) return notFound(section + " is not a valid configuration section");
		  try {
			PropertiesConfigurationHelper.setByKey(conf, key, value);
		  } catch (ConfigurationException e) {
				return badRequest(e.getMessage());
	      }
		  return ok();
	   }

		public static Result getConfiguration(String section) throws  Throwable{
		  response().setContentType("application/json");
		  return ok(PropertiesConfigurationHelper.dumpConfigurationSectionAsFlatJson(section));
		}
		
		public static Result getLatestVersion() {
			String urlToCall="http://www.baasbox.com/version/"+ Internal.INSTALLATION_ID.getValueAsString() + "/";
			Logger.debug("Calling " + urlToCall);
			final Promise<Response> promise = WS.url(urlToCall).get();
			return status(promise.get().getStatus(),promise.get().getBody());
		}//getLatestVersion
			  
		
		public static Result dropDb(Long timeout){
			try{
				DbHelper.shutdownDB();
				if(timeout>0){
					Logger.info(String.format("Sleeping for %d seconds",timeout/1000));
					Thread.sleep(timeout);
				}
			}catch(Exception e){
				Logger.debug(e.getMessage());
				e.printStackTrace();
			}
			return ok();
		}
		
}
