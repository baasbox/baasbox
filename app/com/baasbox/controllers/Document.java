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
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.BasicAuthHeader;
import com.baasbox.controllers.actions.filters.CheckAPPCode;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.ConnectToDB;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;


public class Document extends Controller {
	
		private static String prepareResponseToJson(ODocument doc){
			response().setContentType("application/json");
			return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.DOCUMENT);
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
		@With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class,ExtractQueryParameters.class})
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
	
		@With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class,ExtractQueryParameters.class})
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
		
		
	  @With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class})
	  public static Result getDocument(String collectionName, String rid){
		  Logger.trace("Method Start");
		  Logger.trace("collectionName: " + collectionName);
		  Logger.trace("rid: " + rid);
		  ODocument doc;
		  try {
			doc=DocumentService.get(collectionName, rid);
			if (doc==null) return notFound();
		  }catch (IllegalArgumentException e) {
				return badRequest(e.getMessage()!=null?e.getMessage():"");
		  } catch (InvalidCollectionException e) {
				return notFound(collectionName + " is not a valid collection name");
		  } catch (InvalidModelException e) {
			  return notFound("Document " + rid + " is not a " + collectionName + " document");
		  }catch (ODatabaseException e){
			  return notFound(rid + " unknown");  
		} 
		  Logger.trace("Method End");
		  
		  return ok(prepareResponseToJson(doc));
	  }

	  
	  @With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class})
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
	  
	  /**
	   * Set permission to a document for a given user
	   * @return
	   */
	  public static Result setPermissionToDocumentForAGivenUser(){
		  return status(NOT_IMPLEMENTED);
	  }

	  public static Result setPermissionToDocumentForAGivenRole(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  @With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class})
	  @BodyParser.Of(BodyParser.Json.class)
	  public static Result createDocument(String collection){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  
		  JsonNode bodyJson= body.asJson();
		  Logger.trace("creating document in collection: " + collection);
		  Logger.trace("bodyJson: " + bodyJson);
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
	  
	  @With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class})
	  @BodyParser.Of(BodyParser.Json.class)
	  public static Result updateDocument(String collectionName, String rid){
		  Logger.trace("Method Start");
		  Http.RequestBody body = request().body();
		  JsonNode bodyJson= body.asJson();
		  Logger.trace("updateDocument collectionName: " + collectionName);
		  Logger.trace("updateDocument rid: " + rid);
		  ODocument document=null;
		  try{
			  document=com.baasbox.service.storage.DocumentService.update(collectionName, rid, bodyJson); 
		  }catch (InvalidCollectionException e){
			  return notFound(collectionName + " is not a valid collection name");
		  }catch (InvalidModelException e){
			  return notFound(rid + " is not a valid belongs to " + collectionName);
		  }catch (InvalidParameterException e){
			  return badRequest(rid + " is not a document");
		  }catch (IllegalArgumentException e){
			  return badRequest(rid + " is not a document");  
		  }catch (ODatabaseException e){
			  return notFound(rid + " unknown");  
		  }catch (Throwable e){
			  Logger.error(ExceptionUtils.getFullStackTrace(e));
			  return internalServerError(ExceptionUtils.getFullStackTrace(e));
		  }
		  if (document==null) return notFound("Document " + rid + " was not found in the collection " + collectionName);
		  Logger.trace("Method End");
		  return ok(prepareResponseToJson(document));
	  }
	 
	  
	  @With ({CheckAPPCode.class, BasicAuthHeader.class, ConnectToDB.class})
	  public static Result deleteDocument(String collectionName, String rid){
		  Logger.trace("Method Start");
		  Logger.trace("deleteDocument collectionName: " + collectionName);
		  Logger.trace("deleteDocument rid: " + rid);
		  try {
			DocumentService.delete(collectionName,rid);
		} catch (InvalidCollectionException e) {
			return notFound(e.getMessage());
		} catch (Throwable e ){
			internalServerError(e.getMessage());
		}
		  Logger.trace("Method End");
		  return status(204);
	  }
	  

}
