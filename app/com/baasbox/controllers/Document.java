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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;

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
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.UpdateOldVersionException;
import com.baasbox.enumerations.Permissions;
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
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class Document extends Controller {

	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.DOCUMENT);
	}
	
	private static String prepareResponseToObjectJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.OBJECT);
	}

	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.DOCUMENT);
	}


	/***
	 * 
	 * @param collectionName
	 * @return the number of documents in the collection
	 */
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getCount(String collectionName){
		Logger.trace("Method Start");
		Logger.trace("collectionName: " + collectionName);

		long count;
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			count = DocumentService.getCount(collectionName,criteria);
			Logger.trace("count: " + count);
		} catch (InvalidCollectionException e) {
			Logger.debug (collectionName + " is not a valid collection name");
			return notFound(collectionName + " is not a valid collection name");
		} catch (Exception e){
			Logger.error(ExceptionUtils.getFullStackTrace(e));
			return internalServerError(e.getMessage());
		}
		Logger.trace("Method End");
		response().setContentType("application/json");
		return ok("{\"count\":\""+ count +"\"}");
	}

	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getDocuments(String collectionName){
		Logger.trace("Method Start");
		Logger.trace("collectionName: " + collectionName);

		List<ODocument> result;
		String ret="{[]}";
		try {
			Context ctx=Http.Context.current.get();
			QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
			result = DocumentService.getDocuments(collectionName,criteria);
			Logger.trace("count: " + result.size());
		} catch (InvalidCollectionException e) {
			Logger.debug (collectionName + " is not a valid collection name");
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

		Logger.trace("Method End");
		return ok(ret);
	}		

	private static String getRidByString(String id , boolean isUUID) throws RidNotFoundException {
		String rid="#"+id;
		if (isUUID) {
			Logger.debug("id is an UUID, try to get a valid RID");
			ORID orid=GenericDao.getInstance().getRidByUUID(id);
			if (orid==null) throw new RidNotFoundException(id);
			rid = orid.toString();
			Logger.debug("Retrieved RID: " + rid);
		}
		return rid;
	}

	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result queryDocument(String collectionName,String id,boolean isUUID,String parts){

		if(parts==null || StringUtils.isEmpty(parts)){
			return getDocument(collectionName, id, isUUID);
		} else{
			Logger.trace("Method Start");
			Logger.trace("collectionName: " + collectionName);
			Logger.trace("rid: " + id);
			ODocument doc;
			try {
				String[] tokens = parts.split("/");
				List<Part> queryParts = new ArrayList<Part>();
				PartsLexer pp = new PartsLexer();
				
				try{
				for (int i = 0; i < tokens.length; i++) {
					String p = tokens[i];
					queryParts.add(pp.parse(p, i+1));
				}
				}catch(PartValidationException pve){
					return badRequest(pve.getMessage());
				}
				PartsParser pl = new PartsParser(queryParts);
				String rid = getRidByString(id, isUUID);
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
			}
			Logger.trace("Method End");

			return ok(prepareResponseToObjectJson(doc));
		}
	}

	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result getDocument(String collectionName, String id, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("collectionName: " + collectionName);
			Logger.trace("rid: " + id);
			ODocument doc;
			try {
				String rid = getRidByString(id, isUUID);
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
			Logger.trace("Method End");

			return ok(prepareResponseToJson(doc));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result getDocumentByRid(String rid){
			Logger.trace("Method Start");
			rid="#"+rid;
			Logger.trace("rid: " + rid);
			ODocument doc;
			try {
				doc=DocumentService.get(rid);
				if (doc==null) return notFound();
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			}catch (ODatabaseException e){
				return notFound(rid + " unknown");  
			} 
			Logger.trace("Method End");

			return ok(prepareResponseToJson(doc));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result createDocument(String collection){
			Logger.trace("Method Start");
			Http.RequestBody body = request().body();

			JsonNode bodyJson= body.asJson();
			Logger.trace("creating document in collection: " + collection);
			Logger.trace("bodyJson: " + bodyJson);
			if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
			ODocument document;
			try{
				document=DocumentService.create(collection, bodyJson); 
				Logger.trace("Document created: " + document.getRecord().getIdentity());
			}catch (InvalidCollectionException e){
				return notFound(e.getMessage());
			}catch (Throwable e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result updateDocument(String collectionName, String id, boolean isUUID){
			Logger.trace("Method Start");
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			if (bodyJson.get("@version")!=null && !bodyJson.get("@version").isInt()) return badRequest("@version field must be an Integer");
			Logger.trace("updateDocument collectionName: " + collectionName);
			Logger.trace("updateDocument id: " + id);
			if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
			ODocument document=null;
			try{
				String rid="#"+id;
				if (isUUID) {
					Logger.debug("id is an UUID, try to get a valid RID");
					ORID orid=GenericDao.getInstance().getRidByUUID(id);
					if (orid==null) return notFound("UUID " + id + " not found");
					rid = orid.toString();
					Logger.debug("Retrieved RID: " + rid);
				}
				document=com.baasbox.service.storage.DocumentService.update(collectionName, rid, bodyJson);   
			}catch (UpdateOldVersionException e){
				return status(CustomHttpCode.DOCUMENT_VERSION.getBbCode(),"You are attempting to update an older version of the document. Your document version is " + e.getVersion1() + ", the stored document has version " + e.getVersion2());	
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
			}catch (Throwable e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			if (document==null) return notFound("Document " + id + " was not found in the collection " + collectionName);
			Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		@BodyParser.Of(BodyParser.Json.class)
		public static Result updateDocumentWithParts(String collectionName, String id, boolean isUUID,String parts){
			Logger.trace("Method Start");
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			Logger.trace("updateDocument collectionName: " + collectionName);
			Logger.trace("updateDocument id: " + id);
			if (bodyJson==null) return badRequest("The body payload cannot be empty. Hint: put in the request header Content-Type: application/json");
			if (bodyJson.get("data")==null) return badRequest("The body payload must have a data field. Hint: modify your content to have a \"data\" field");
			ODocument document=null;
			try{
				String rid=getRidByString(id, isUUID);
				String[] tokens = parts.split("/");
				PartsLexer lexer = new PartsLexer();
				List<Part> objParts = new ArrayList<Part>();
				for (int i = 0; i < tokens.length; i++) {
					try{
						objParts.add(lexer.parse(tokens[i], i+1));
					}catch(PartValidationException pve){
						return badRequest(pve.getMessage());
					}
				}
				PartsParser pp = new PartsParser(objParts);
				document=com.baasbox.service.storage.DocumentService.update(collectionName, rid, bodyJson,pp);   
			}catch (MissingNodeException e){
				return notFound(e.getMessage());
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
			}catch (RidNotFoundException rnfe){
					return notFound(rnfe.getMessage());	
			}catch (Throwable e){
				Logger.error(ExceptionUtils.getFullStackTrace(e));
				return internalServerError(ExceptionUtils.getFullStackTrace(e));
			}
			if (document==null) return notFound("Document " + id + " was not found in the collection " + collectionName);
			Logger.trace("Method End");
			return ok(prepareResponseToJson(document));
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result deleteDocument(String collectionName, String id, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("deleteDocument collectionName: " + collectionName);
			Logger.trace("deleteDocument rid: " + id);
			try {
				String rid="#"+id;
				if (isUUID) {
					Logger.debug("id is an UUID, try to get a valid RID");
					ORID orid=GenericDao.getInstance().getRidByUUID(id);
					if (orid==null) return notFound("UUID " + id + " not found");
					rid = orid.toString();
					Logger.debug("Retrieved RID: " + rid);
				}
				DocumentService.delete(collectionName,rid);
			}catch (OSecurityException e){
				return forbidden("You have not the right to delete " + id);  
			} catch (InvalidCollectionException e) {
				return notFound(e.getMessage());
			} catch (Throwable e ){
				internalServerError(e.getMessage());
			}
			Logger.trace("Method End");
			return ok("");
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result grantToUser(String collectionName, String rid, String username, String action, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("grant collectionName: " + collectionName);
			Logger.trace("grant rid: " + rid);
			Logger.trace("grant username: " + username);
			Logger.trace("grant action: " + action);
			Result res=grantOrRevokeToUser(collectionName,rid,username,action,true, isUUID);
			Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result revokeToUser(String collectionName, String rid, String username, String action, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("grant collectionName: " + collectionName);
			Logger.trace("grant rid: " + rid);
			Logger.trace("grant username: " + username);
			Logger.trace("grant action: " + action);	  
			Result res=grantOrRevokeToUser(collectionName,rid,username,action,false, isUUID);
			Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result grantToRole(String collectionName, String rid, String rolename, String action, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("grant collectionName: " + collectionName);
			Logger.trace("grant rid: " + rid);
			Logger.trace("grant rolename: " + rolename);
			Logger.trace("grant action: " + action);
			Result res=grantOrRevokeToRole(collectionName,rid,rolename,action,true, isUUID);
			Logger.trace("Method End");
			return res;
		}

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result revokeToRole(String collectionName, String rid, String rolename, String action, boolean isUUID){
			Logger.trace("Method Start");
			Logger.trace("grant collectionName: " + collectionName);
			Logger.trace("grant rid: " + rid);
			Logger.trace("grant rolename: " + rolename);
			Logger.trace("grant action: " + action);	  
			Result res=grantOrRevokeToRole(collectionName,rid,rolename,action,false, isUUID);
			Logger.trace("Method End");
			return res;
		}

		private static Result grantOrRevokeToUser(String collectionName, String id,
				String username, String action, boolean grant, boolean isUUID) {
			try {
				//converts uuid in rid
				String rid="#"+id;
				if (isUUID) {
					Logger.debug("id is an UUID, try to get a valid RID");
					ORID orid=GenericDao.getInstance().getRidByUUID(id);
					if (orid==null) return notFound("UUID " + id + " not found");
					rid = orid.toString();
					Logger.debug("Retrieved RID: " + rid);
				}			
				Permissions permission=PermissionsHelper.permissionsFromString.get(action.toLowerCase());
				if (permission==null) return badRequest(action + " is not a valid action");
				if (grant) DocumentService.grantPermissionToUser(collectionName, rid, permission, username);
				else       DocumentService.revokePermissionToUser(collectionName, rid, permission, username);
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
				String rid="#"+id;
				if (isUUID) {
					Logger.debug("id is an UUID, try to get a valid RID");
					ORID orid=GenericDao.getInstance().getRidByUUID(id);
					if (orid==null) return notFound("UUID " + id + " not found");
					rid = orid.toString();
					Logger.debug("Retrieved RID: " + rid);
				}
				if (grant) DocumentService.grantPermissionToRole(collectionName, rid, permission, rolename);
				else       DocumentService.revokePermissionToRole(collectionName, rid, permission, rolename);
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
	}
