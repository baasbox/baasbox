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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.simple.JSONObject;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Response;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.FileTooBigException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.service.storage.FileService;
import com.baasbox.service.storage.StorageUtils;
import com.baasbox.service.storage.StorageUtils.ImageDimensions;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;



public class File extends Controller {
	private static final String FILE_FIELD_NAME="file";
	private static final String DATA_FIELD_NAME="attachedData";
	private static final String QUERY_STRING_FIELD_DOWNLOAD="download";
	private static final String QUERY_STRING_FIELD_RESIZE="resize";
	private static final String QUERY_STRING_FIELD_RESIZE_ID="sizeId";
	private static final Object ACL_FIELD_NAME = "acl";
	
	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.FILE);
	}
	
	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.FILE);
	}
	
	
	  /*------------------FILE--------------------*/
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	  public static Result storeFile() throws  Throwable {
		  MultipartFormData  body = request().body().asMultipartFormData();
			if (body==null) return badRequest("missing data: is the body multipart/form-data? Check if it contains boundaries too! " );
			//FilePart file = body.getFile(FILE_FIELD_NAME);
			List<FilePart> files = body.getFiles();
			FilePart file = null;
			if (!files.isEmpty()) file = files.get(0);
			String ret="";
			if (file!=null){
				Map<String, String[]> data=body.asFormUrlEncoded();
				String[] datas=data.get(DATA_FIELD_NAME);
				String[] acl=data.get(ACL_FIELD_NAME);

				/*extract attachedData */
				String dataJson=null;
				if (datas!=null && datas.length>0){
					dataJson = datas[0];
				}else dataJson="{}";
				
				/*extract acl*/
				/*the acl json must have the following format:
				 * {
				 * 		"read" : {
				 * 					"users":[],
				 * 					"roles":[]
				 * 				 }
				 * 		"update" : .......
				 * }
				 */
				String aclJsonString=null;
				if (acl!=null && datas.length>0){
					aclJsonString = acl[0];
					ObjectMapper mapper = new ObjectMapper();
					JsonNode aclJson=null;
					try{
						aclJson = mapper.readTree(aclJsonString);
					}catch(JsonProcessingException e){
						return status(CustomHttpCode.ACL_JSON_FIELD_MALFORMED.getBbCode(),"The 'acl' field is malformed");
					}
					/*check if the roles and users are valid*/
					 Iterator<Entry<String, JsonNode>> it = aclJson.fields();
					 while (it.hasNext()){
						 //check for permission read/update/delete/all
						 Entry<String, JsonNode> next = it.next();
						 if (!PermissionsHelper.permissionsFromString.containsKey(next.getKey())){
							 return status(CustomHttpCode.ACL_PERMISSION_UNKNOWN.getBbCode(),"The key '"+next.getKey()+"' is invalid. Valid ones are 'read','update','delete','all'");
						 }
						 //check for users/roles
						 Iterator<Entry<String, JsonNode>> it2 = next.getValue().fields();
						 while (it2.hasNext()){
							 Entry<String, JsonNode> next2 = it2.next();
							 if (!next2.getKey().equals("users") && !next2.getKey().equals("roles")) {
								 return status(CustomHttpCode.ACL_USER_OR_ROLE_KEY_UNKNOWN.getBbCode(),"The key '"+next2.getKey()+"' is invalid. Valid ones are 'users' or 'roles'");
							 }
							 //check for the existance of users/roles
							 JsonNode arrNode = next2.getValue();
							 if (arrNode.isArray()) {
								    for (final JsonNode objNode : arrNode) {
								        //checks the existance users and/or roles
								    	if (next2.getKey().equals("users") && !UserService.exists(objNode.asText())) return status(CustomHttpCode.ACL_USER_DOES_NOT_EXIST.getBbCode(),"The user " + objNode.asText() + " does not exists");
								    	if (next2.getKey().equals("roles") && !RoleService.exists(objNode.asText())) return status(CustomHttpCode.ACL_ROLE_DOES_NOT_EXIST.getBbCode(),"The role " + objNode.asText() + " does not exists");
								    	
								    }
							 }else return status(CustomHttpCode.JSON_VALUE_MUST_BE_ARRAY.getBbCode(),"The '"+next2.getKey()+"' value must be an array");
						 }
						 
					 }
					
				}else aclJsonString="{}";
				
				
				
			    java.io.File fileContent=file.getFile();
				String fileName = file.getFilename();
			   /*String contentType = file.getContentType(); 
			    if (contentType==null || contentType.isEmpty() || contentType.equalsIgnoreCase("application/octet-stream")){	//try to guess the content type
			    	InputStream is = new BufferedInputStream(new FileInputStream(fileContent));
			    	contentType = URLConnection.guessContentTypeFromStream(is);
			    	if (contentType==null || contentType.isEmpty()) contentType="application/octet-stream";
			    }*/
				InputStream is = new FileInputStream(fileContent);
		    	/* extract file metadata and content */
		    	try{
		    	    BodyContentHandler contenthandler = new BodyContentHandler(-1);
		    		//DefaultHandler contenthandler = new DefaultHandler();
			        Metadata metadata = new Metadata();
			        metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
			        Parser parser = new AutoDetectParser();
			        parser.parse(is, contenthandler, metadata,new ParseContext());			        
			        String contentType =  metadata.get(Metadata.CONTENT_TYPE);
			       	if (StringUtils.isEmpty(contentType)) contentType="application/octet-stream";
			       	
			        HashMap<String,Object> extractedMetaData = new HashMap<String,Object>();
			        for (String key:metadata.names()){
			        	try{
			        	if (metadata.isMultiValued(key)){
			        		if (Logger.isDebugEnabled()) Logger.debug(key + ": ");
			        		for (String value: metadata.getValues(key)){
			        			if (Logger.isDebugEnabled()) Logger.debug("   " + value);
			        		}
			        		extractedMetaData.put(key.replace(":", "_").replace(" ", "_").trim(), Arrays.asList(metadata.getValues(key)));
			        	}else{
			        		if (Logger.isDebugEnabled()) Logger.debug(key + ": " + metadata.get(key));
			        		extractedMetaData.put(key.replace(":", "_").replace(" ", "_").trim(), metadata.get(key));
			        	}
			        	}catch(Throwable e){
			        		Logger.warn("Unable to extract metadata for file " + fileName + ", key " + key);
			        	}
			        }
			      

			        if (Logger.isDebugEnabled()) Logger.debug(".................................");
			        if (Logger.isDebugEnabled()) Logger.debug(new JSONObject(extractedMetaData).toString());
			    	
			        is.close();
			    	is=new FileInputStream(fileContent);
			    	ODocument doc=FileService.createFile(
			    			fileName,
			    			dataJson,
			    			aclJsonString,
			    			contentType, 
			    			fileContent.length(), 
			    			is,
			    			extractedMetaData,
			    			contenthandler.toString());
			    	ret=prepareResponseToJson(doc); 
		    	}catch ( JsonProcessingException e) {
		    		throw new Exception ("Error parsing acl field. HINTS: is it a valid JSON string?", e);
				}catch (Throwable e){
					Logger.error("Error parsing uploaded file",e);
		    		throw new Exception ("Error parsing uploaded file", e);
		    	} finally{
		    		if (is != null) is.close();
		    	}
			}else{
				return badRequest("missing the file data in the body payload");
			}
		  return created(ret);
	  }//storeFile()
	  

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result deleteFile(String id) throws Throwable{
		try{
			FileService.deleteById(id);
		}catch(FileNotFoundException e){
			return notFound(id + " file not found");
		}
		return ok();
	}
	
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class})
	  public static Result getFileAttachedData(String id) throws IOException{
			ODocument doc;
			try {
				doc = FileService.getById(id);
			} catch (SqlInjectionException e) {
				return badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)");
			} catch (InvalidModelException e) {
				return badRequest("The id " + id + " is not a file");
			}
			if (doc==null) return notFound(id + " file was not found");
			String ret=OJSONWriter.writeValue(doc.rawField(FileService.DATA_FIELD_NAME));
			return ok(ret);
	  }
	  
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getAllFile() throws IOException{
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		List<ODocument> listOfFiles;
		try {
			listOfFiles = FileService.getFiles(criteria);
		} catch (InvalidCriteriaException e) {
			return badRequest(e.getMessage()!=null?e.getMessage():"");
		} catch (SqlInjectionException e) {
			return badRequest("the supplied criteria appear invalid (Sql Injection Attack detected)");
		}
		return ok(prepareResponseToJson(listOfFiles));
	}
	
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getFileContent(String id){
		String theContent="";

			try {
				theContent=FileService.getExtractedContent(id);
			} catch (SqlInjectionException e) {
				return badRequest("The querystring is malformed or not well encoded");
			} catch (InvalidModelException e) {
				return badRequest("The id " + id + " is not a file");
			} catch (FileNotFoundException e) {
				return notFound("The file " + id + " was not found");
			}
		response().setHeader(Response.CONTENT_TYPE, "text/plain");
		return ok(theContent);
	}
	
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getFile(String id){
		
		ODocument doc;
		try {
			doc = FileService.getById(id);
		} catch (SqlInjectionException e) {
			return badRequest("the supplied id appears invalid (possible Sql Injection Attack detected)");
		} catch (InvalidModelException e) {
			return badRequest("The id " + id + " is not a file");
		}
		if (doc==null) return notFound(id + " file was not found");
		return ok(prepareResponseToJson(doc));
	}
	
	  @With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class})
	  public static Result streamFile(String id) throws IOException, InvalidModelException{
			try {
				ODocument doc=FileService.getById(id);
				if (doc==null) return notFound(id + " file was not found");
				String filename=(String)doc.field("fileName");
				
				Context ctx=Http.Context.current.get();
				Boolean download = BooleanUtils.toBoolean(ctx.request().getQueryString(QUERY_STRING_FIELD_DOWNLOAD));
				String resize = ctx.request().getQueryString(QUERY_STRING_FIELD_RESIZE);
				boolean resizeIsEmpty=StringUtils.isEmpty(resize);
				Integer sizeId = Ints.tryParse(ctx.request().getQueryString(QUERY_STRING_FIELD_RESIZE_ID)+"");
				
				byte[] output;
				if (sizeId!=null){
					output = FileService.getResizedPicture(id, sizeId);
					String[] fileName=((String)doc.field("fileName")).split("\\.");
					filename=fileName[0] + "_" + sizeId + "." + (fileName.length>1?fileName[1]:"");
				}else if (!resizeIsEmpty){
					if (!ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().contains(resize) && !UserService.userCanByPassRestrictedAccess(DbHelper.currentUsername()))
						throw new InvalidSizePatternException("The requested resize format is not allowed");
					ImageDimensions imgDim = StorageUtils.convertPatternToDimensions(resize);
					output = FileService.getResizedPicture(id, imgDim);
					String[] fileName=((String)doc.field("fileName")).split("\\.");
					filename=fileName[0] + "_" + resize + "." + (fileName.length>1?fileName[1]:"");
				}else{
					ORecordBytes record = doc.field(FileService.BINARY_FIELD_NAME);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					record.toOutputStream(out);
					output=out.toByteArray();
				}
				response().setContentType((String)doc.field(FileService.CONTENT_TYPE_FIELD_NAME));
				response().setHeader("Content-Length", String.valueOf(output.length));
				if (download) response().setHeader("Content-Disposition", "attachment; filename=\""+URLEncoder.encode(filename,"UTF-8")+"\"");
				//return ok(new ByteArrayInputStream(output));
				return ok(output);
			} catch (SqlInjectionException e) {
				return badRequest("the supplied id appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				Logger.error("error retrieving file content " + id, e);
				throw e;
			} catch (DocumentIsNotAnImageException e){
				return badRequest("The id "+id+"is not an image and cannot be resize");
			} catch(DocumentIsNotAFileException e){
				return badRequest("The id "+id+"is not a file");
			} catch(InvalidSizePatternException e){
				return badRequest("The resize parameters are not valid");
			} catch(InvalidModelException e) {
				throw e;
			} catch(DocumentNotFoundException e) {
				return notFound("The requested file does not exists: " + id);
			} catch(FileTooBigException e) {
				return status(503,"The requested image is too big to be processed now. Try later. File: " + id);
			}
	  }//streamFile
	  

	  
		@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
		public static Result grantOrRevokeToRole(String id,String rolename, String action, boolean grant) {
			try {
				Permissions permission=PermissionsHelper.permissionsFromString.get(action.toLowerCase());
				if (grant) FileService.grantPermissionToRole(id, permission, rolename);
				else       FileService.revokePermissionToRole(id, permission, rolename);
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			} catch (RoleNotFoundException e) {
				return notFound("role " + rolename + " not found");
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
		public static Result grantOrRevokeToUser(String id, String username, String action, boolean grant) {
			try {
				Permissions permission=PermissionsHelper.permissionsFromString.get(action.toLowerCase());
				if (grant) FileService.grantPermissionToUser(id, permission, username);
				else       FileService.revokePermissionToUser(id, permission, username);
			} catch (IllegalArgumentException e) {
				return badRequest(e.getMessage());
			} catch (RoleNotFoundException e) {
				return notFound("user " + username + " not found");
			} catch (OSecurityAccessException e ){
				return Results.forbidden();
			} catch (OSecurityException e ){
				return Results.forbidden();				
			} catch (Throwable e ){
				return internalServerError(e.getMessage());
			}
			return ok();
		}//grantOrRevokeToUser
	

		
	  public static Result updateAttachedData(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
}