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
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import com.baasbox.controllers.actions.exceptions.RidNotFoundException;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.dao.PermissionJsonWrapper;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.UpdateOldVersionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.query.MissingNodeException;
import com.baasbox.service.query.PartsLexer;
import com.baasbox.service.query.PartsLexer.Part;
import com.baasbox.service.query.PartsLexer.PartValidationException;
import com.baasbox.service.query.PartsParser;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.JSONFormats.Formats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class Document extends Controller {

	private static final String JSON_BODY_NULL = "The body payload cannot be empty. Hint: put in the request header Content-Type: application/json";


	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		Formats format;
		try{
			DbHelper.filterOUserPasswords(true);
			if (BooleanUtils.toBoolean(Http.Context.current().request().getQueryString("withAcl")))
			{
				format=JSONFormats.Formats.DOCUMENT_WITH_ACL;
				return JSONFormats.prepareResponseToJson(doc,format,true);
			}
			else
			{
				format=JSONFormats.Formats.DOCUMENT;
				return JSONFormats.prepareResponseToJson(doc,format,false);
			}
		}finally{
			DbHelper.filterOUserPasswords(false);
		}
	}
	
	private static String prepareResponseToObjectJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.OBJECT);
	}

	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		Formats format;
		try{
			DbHelper.filterOUserPasswords(true);
			if (BooleanUtils.toBoolean(Http.Context.current().request().getQueryString("withAcl"))){
				format=JSONFormats.Formats.DOCUMENT_WITH_ACL;
				return  JSONFormats.prepareResponseToJson(listOfDoc,format,true);
			}else{
				format=JSONFormats.Formats.DOCUMENT;
				return  JSONFormats.prepareResponseToJson(listOfDoc,format);
			}
		}finally{
			DbHelper.filterOUserPasswords(false);
		}
	}


	/***
	 * 
	 * @param collectionName
	 * @return the number of documents in the collection
	 */
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getCount(String collectionName){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (Logger.isTraceEnabled()) Logger.trace("collectionName: " + collectionName);

		long count;
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			count = DocumentService.getCount(collectionName,criteria);
			if (Logger.isTraceEnabled()) Logger.trace("count: " + count);
		} catch (InvalidCollectionException e) {
			if (Logger.isDebugEnabled()) Logger.debug (collectionName + " is not a valid collection name");
			return notFound(collectionName + " is not a valid collection name");
		} catch (Exception e){
			Logger.error(ExceptionUtils.getFullStackTrace(e));
			return internalServerError(e.getMessage());
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		response().setContentType("application/json");
		return ok("{\"count\": "+ count +" }");
	}

	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getDocuments(String collectionName){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (Logger.isTraceEnabled()) Logger.trace("collectionName: " + collectionName);

		List<ODocument> result;
		String ret="{[]}";
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			result = DocumentService.getDocuments(collectionName,criteria);
			if (Logger.isTraceEnabled()) Logger.trace("count: " + result.size());
		} catch (InvalidCollectionException e) {
			if (Logger.isDebugEnabled()) Logger.debug (collectionName + " is not a valid collection name");
			return notFound(collectionName + " is not a valid collection name");
		} catch (Exception e){
			Logger.error(ExceptionUtils.getFullStackTrace(e));
			return internalServerError(e.getMessage());
		}

		try{
			ret=prepareResponseToJson(result);
		}catch (IOException e){
			return internalServerError(ExceptionUtils.getFullStackTrace(e));
		}

		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return ok(ret);
	}

    @With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result queryDocument(String collectionName,String id,boolean isUUID,String parts){
		if(parts==null || StringUtils.isEmpty(parts)){
			return getDocument(collectionName, id, isUUID);
		} else{
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("rid: " + id);
			ODocument doc;
			try {
				
				String[] tokens = parts.split("/");
				List<Part> queryParts = new ArrayList<Part>();
				PartsLexer pp = new PartsLexer();
				
				try{
				for (int i = 0; i < tokens.length; i++) {
					try{
						String p = java.net.URLDecoder.decode(tokens[i], "UTF-8");
						queryParts.add(pp.parse(p, i+1));
					}catch(Exception e){
						return badRequest("Unable to decode parts");
					}
				}
				}catch(PartValidationException pve){
					return badRequest(pve.getMessage());
				}
				PartsParser pl = new PartsParser(queryParts);
				String rid = DocumentService.getRidByString(id, isUUID);
				doc=DocumentService.get(collectionName, rid,pl);
				
				if (doc==null) return notFound();
			}catch (IllegalArgumentException e) {
				return badRequest(e.getMessage()!=null?e.getMessage():"");
			} catch (InvalidCollectionException e) {
				return notFound(collectionName + " is not a valid collection name");
			} catch (InvalidModelException e) {
				return notFound("Document " + id + " is not a " + collectionName + " document");
			}catch (ODatabaseException e){
				return notFound(id + " not found. Do you have the right to read it?");  
			} catch (DocumentNotFoundException e) {
				return notFound(id + " not found"); 
			} catch (RidNotFoundException e) {
				return notFound(e.getMessage());
			} catch (InvalidCriteriaException e) {
				return badRequest(e.getMessage()!=null?e.getMessage():"");
			}
			if (Logger.isTraceEnabled()) Logger.trace("Method End");

			return ok(prepareResponseToObjectJson(doc));
		}
	}

	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result getDocument(String collectionName, String id, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("rid: " + id);
			ODocument doc;
			try {
				String rid = DocumentService.getRidByString(id, isUUID);
				doc=DocumentService.get(collectionName, rid);
				if (doc==null) return notFound();
			}catch (IllegalArgumentException e) {
				return badRequest(e.getMessage()!=null?e.getMessage():"");
			} catch (InvalidCollectionException e) {
				return notFound(collectionName + " is not a valid collection name");
			} catch (InvalidModelException e) {
				return notFound("Document " + id + " is not a " + collectionName + " document");
			}catch (ODatabaseException e){
				return notFound(id + " not found. Do you have the right to read it?");  
			} catch (DocumentNotFoundException e) {
				return notFound(id + " not found"); 
			} catch (RidNotFoundException e) {
				return notFound(e.getMessage()); 
			} 
			if (Logger.isTraceEnabled()) Logger.trace("Method End");

			return ok(prepareResponseToJson(doc));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result getDocumentByRid(String rid){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			rid="#"+rid;
			if (Logger.isTraceEnabled()) Logger.trace("rid: " + rid);
			ODocument doc;
			try {
				doc=DocumentService.get(rid);
				if (doc==null) return notFound();
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			}catch (ODatabaseException e){
				return notFound(rid + " unknown");  
			} 
			if (Logger.isTraceEnabled()) Logger.trace("Method End");

			return ok(prepareResponseToJson(doc));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result createDocument(String collection){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			Http.RequestBody body = request().body();
			ODocument document=null;
			try{
				JsonNode bodyJson= body.asJson();
				if (!bodyJson.isObject()) throw new InvalidJsonException("The body must be an JSON object");
				if (Logger.isTraceEnabled()) Logger.trace("creating document in collection: " + collection);
				if (Logger.isTraceEnabled()) Logger.trace("bodyJson: " + bodyJson);
				if (bodyJson==null) return badRequest(JSON_BODY_NULL);
				document=DocumentService.create(collection, (ObjectNode)bodyJson); 
				if (Logger.isTraceEnabled()) Logger.trace("Document created: " + document.getRecord().getIdentity());
			}catch (InvalidCollectionException e){
				return notFound(e.getMessage());
			}catch (InvalidJsonException e){
				return badRequest("JSON not valid. HINT: check if it is not just a JSON collection ([..]), a single element ({\"element\"}) or you are trying to pass a @version:null field");
			}catch (UpdateOldVersionException e){
				return badRequest(ExceptionUtils.getMessage(e));
			} catch (InvalidModelException e) {
				return badRequest("ACL fields are not valid: " + e.getMessage());
			}catch (Throwable e){
					Logger.error(ExceptionUtils.getFullStackTrace(e));
					return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result updateDocument(String collectionName, String id, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			if (bodyJson==null) return badRequest(JSON_BODY_NULL);
			if (bodyJson.get("@version")!=null && !bodyJson.get("@version").isInt()) return badRequest("@version field must be an Integer");
			ODocument document=null;
			try{
				if (!bodyJson.isObject()) throw new InvalidJsonException("The body must be an JSON object");
				if (Logger.isTraceEnabled()) Logger.trace("updateDocument collectionName: " + collectionName);
				if (Logger.isTraceEnabled()) Logger.trace("updateDocument id: " + id);
				String rid= DocumentService.getRidByString(id, isUUID);
				document=com.baasbox.service.storage.DocumentService.update(collectionName, rid, (ObjectNode)bodyJson);
			} catch (DocumentNotFoundException e) {
				return notFound("Document " + id + " not found");
			}catch (AclNotValidException e){
				return badRequest("ACL fields are not valid: " + e.getMessage());		
			}catch (UpdateOldVersionException e){
				return status(CustomHttpCode.DOCUMENT_VERSION.getBbCode(),"You are attempting to update an older version of the document. Your document version is " + e.getVersion1() + ", the stored document has version " + e.getVersion2());	
			}catch (RidNotFoundException e){
				return notFound("id " + id + " not found");
			}catch (InvalidCollectionException e){
				return notFound(collectionName + " is not a valid collection name");
			}catch (InvalidModelException e){
				return notFound(id + " is not a valid belongs to " + collectionName);
			}catch (InvalidParameterException e){
				return badRequest(id + " is not a document");
			}catch (IllegalArgumentException e){
				return badRequest(id + " is not a document");  
			}catch (ODatabaseException e){
				return notFound(id + " unknown");  
			}catch (OSecurityException e){
				return forbidden("You have not the right to modify " + id);  
			}catch (InvalidJsonException e){
				return badRequest("JSON not valid. HINT: check if it is not just a JSON collection ([..]), a single element ({\"element\"}) or you are trying to pass a @version:null field");				
			}catch (Throwable e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			if (document==null) return notFound("Document " + id + " was not found in the collection " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result updateDocumentWithParts(String collectionName, String id, boolean isUUID,String parts){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			if (Logger.isTraceEnabled()) Logger.trace("updateDocument collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("updateDocument id: " + id);
			if (!bodyJson.isObject()) return badRequest("The body must be an JSON object");
			if (bodyJson==null) return badRequest(JSON_BODY_NULL);
			if (bodyJson.get("data")==null) return badRequest("The body payload must have a data field. Hint: modify your content to have a \"data\" field");
			ODocument document=null;
			try{
				String rid= DocumentService.getRidByString(id, isUUID);
				String[] tokens = parts.split("/");
				PartsLexer lexer = new PartsLexer();
				List<Part> objParts = new ArrayList<Part>();
				for (int i = 0; i < tokens.length; i++) {
					try{
						String p = java.net.URLDecoder.decode(tokens[i], "UTF-8");
						objParts.add(lexer.parse(p, i+1));
					}catch(PartValidationException pve){
						return badRequest(pve.getMessage());
					}catch(Exception e){
						return badRequest("Unable to parse document parts");
					}
				}
				PartsParser pp = new PartsParser(objParts);
				document=com.baasbox.service.storage.DocumentService.update(collectionName, rid, (ObjectNode)bodyJson,pp);   
			}catch (MissingNodeException e){
				return notFound(e.getMessage());
			}catch (InvalidCollectionException e){
				return notFound(collectionName + " is not a valid collection name");
			}catch (InvalidModelException e){
				return notFound(id + " is not a valid belongs to " + collectionName);
			}catch (InvalidParameterException e){
				return notFound("Cannot find the document " + id + " HINT: have you the permission to access this document?");
			}catch (IllegalArgumentException e){
				return badRequest(id + " is not a document");  
			}catch (ODatabaseException e){
				return notFound(id + " unknown");  
			}catch (OSecurityException e){
				return forbidden("You have not the right to modify the document " + id);
			}catch (RidNotFoundException rnfe){
					return notFound(rnfe.getMessage());	
			}catch (Throwable e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			if (document==null) return notFound("Document " + id + " was not found in the collection " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result deleteDocument(String collectionName, String id, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("deleteDocument collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("deleteDocument rid: " + id);
			try {
				String rid= DocumentService.getRidByString(id, isUUID);
				DocumentService.delete(collectionName,rid);
			}catch (RidNotFoundException e){
				return notFound("id  " + id + " not found");  
			}catch (OSecurityException e){
				return forbidden("You have not the right to delete " + id);  
			} catch (InvalidCollectionException e) {
				return notFound(e.getMessage());
			} catch (Throwable e ){
				internalServerError(e.getMessage());
			}
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return ok("");
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result grantToUser(String collectionName, String rid, String username, String action, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("grant collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("grant rid: " + rid);
			if (Logger.isTraceEnabled()) Logger.trace("grant username: " + username);
			if (Logger.isTraceEnabled()) Logger.trace("grant action: " + action);
			Result res=grantOrRevokeToUser(collectionName,rid,username,action,true, isUUID);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result revokeToUser(String collectionName, String rid, String username, String action, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("grant collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("grant rid: " + rid);
			if (Logger.isTraceEnabled()) Logger.trace("grant username: " + username);
			if (Logger.isTraceEnabled()) Logger.trace("grant action: " + action);	  
			Result res=grantOrRevokeToUser(collectionName,rid,username,action,false, isUUID);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result grantToRole(String collectionName, String rid, String rolename, String action, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("grant collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("grant rid: " + rid);
			if (Logger.isTraceEnabled()) Logger.trace("grant rolename: " + rolename);
			if (Logger.isTraceEnabled()) Logger.trace("grant action: " + action);
			Result res=grantOrRevokeToRole(collectionName,rid,rolename,action,true, isUUID);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result revokeToRole(String collectionName, String rid, String rolename, String action, boolean isUUID){
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			if (Logger.isTraceEnabled()) Logger.trace("grant collectionName: " + collectionName);
			if (Logger.isTraceEnabled()) Logger.trace("grant rid: " + rid);
			if (Logger.isTraceEnabled()) Logger.trace("grant rolename: " + rolename);
			if (Logger.isTraceEnabled()) Logger.trace("grant action: " + action);	  
			Result res=grantOrRevokeToRole(collectionName,rid,rolename,action,false, isUUID);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return res;
		}

		private static Result grantOrRevokeToUser(String collectionName, String id,
				String username, String action, boolean grant, boolean isUUID) {
			try {
				//converts uuid in rid
				String rid= DocumentService.getRidByString(id, isUUID);
				Permissions permission=PermissionsHelper.permissionsFromString.get(action.toLowerCase());
				if (permission==null) return badRequest(action + " is not a valid action");
				if (grant) DocumentService.grantPermissionToUser(collectionName, rid, permission, username);
				else       DocumentService.revokePermissionToUser(collectionName, rid, permission, username);
			} catch (RidNotFoundException e) {
				return notFound("id " + id + " not found");
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			} catch (UserNotFoundException e) {
				return notFound("user " + username + " not found");
			} catch (InvalidCollectionException e) {
				return notFound("collection " + collectionName + " not found");
			} catch (InvalidModelException e) {
				return badRequest(id + " does not belong to the " + collectionName + " collection");
			} catch (DocumentNotFoundException e) {
				return notFound("document " + id + " not found. Hint: has the user the correct rights on it?");
			} catch (OSecurityAccessException e ){
				return Results.forbidden();
			} catch (OSecurityException e ){
				return Results.forbidden();				
			} catch (Throwable e ){
				return internalServerError(e.getMessage());
			}
			return ok();
		}//grantOrRevokeToUser

		private static Result grantOrRevokeToRole(String collectionName, String id,
				String rolename, String action, boolean grant, boolean isUUID) {
			try {
				Permissions permission=PermissionsHelper.permissionsFromString.get(action.toLowerCase());
				if (permission==null) return badRequest(action + " is not a valid action");
				//converts uuid in rid
				String rid = DocumentService.getRidByString(id, isUUID);
				if (grant) DocumentService.grantPermissionToRole(collectionName, rid, permission, rolename);
				else       DocumentService.revokePermissionToRole(collectionName, rid, permission, rolename);
			} catch (RidNotFoundException e) {
				return notFound("id " + id + " no found");
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			} catch (RoleNotFoundException e) {
				return notFound("role " + rolename + " not found");
			} catch (InvalidCollectionException e) {
				return notFound("collection " + collectionName + " not found");
			} catch (InvalidModelException e) {
				return badRequest(id + " does not belong to the " + collectionName + " collection");
			} catch (DocumentNotFoundException e) {
				return notFound("document " + id + " not found. Hint: has the user the correct rights on it?");
			} catch (OSecurityAccessException e ){
				return Results.forbidden();
			} catch (OSecurityException e ){
				return Results.forbidden();				
			} catch (Throwable e ){
				return internalServerError(e.getMessage());
			}
			return ok();
		}//grantOrRevokeToRole
		
		
		@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result updateAcl(String collectionName, String uuid){
			Http.RequestBody body = request().body();
			ObjectNode bodyJson= (ObjectNode)body.asJson();
			if (bodyJson==null) return badRequest(JSON_BODY_NULL);
			PermissionJsonWrapper acl;
			ODocument doc=null;
			try {
				acl = new PermissionJsonWrapper(bodyJson, true);
				doc=DocumentService.setAcl(collectionName, uuid, acl);
			} catch (AclNotValidException e) {
				return badRequest("ACL fields are not valid: " + e.getMessage());
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			} catch (InvalidCollectionException e) {
				return notFound("collection " + collectionName + " not found");
			} catch (InvalidModelException e) {
				return badRequest(uuid + " does not belong to the " + collectionName + " collection");
			} catch (DocumentNotFoundException e) {
				return notFound("document " + uuid + " not found. Hint: has the user the correct rights on it?");
			} catch (OSecurityAccessException e ){
				return Results.forbidden();
			} catch (OSecurityException e ){
				return Results.forbidden();				
			} catch (Throwable e ){
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			return ok(prepareResponseToJson(doc));
		}
		
	}
