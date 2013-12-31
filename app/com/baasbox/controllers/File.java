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

<<<<<<< HEAD
=======
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
>>>>>>> upstream/master
import play.mvc.Controller;
import play.mvc.Result;
<<<<<<< HEAD


public class File extends Controller {
	  /*------------------FILE--------------------*/
	  public static Result storeFile(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result getFileMetadata(){
=======
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
import com.google.common.io.Files;
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
			Map<String, String[]> data=body.asFormUrlEncoded();
			String[] datas=data.get(DATA_FIELD_NAME);
			String ret="";
			if (file!=null){
				String dataJson=null;
				if (datas!=null && datas.length>0){
					dataJson = datas[0];
				}else dataJson="{}";
			    java.io.File fileContent=file.getFile();
			    byte [] fileContentAsByteArray=Files.toByteArray(fileContent);
				String fileName = file.getFilename();
			    String contentType = file.getContentType(); 
			    if (contentType==null || contentType.isEmpty() || contentType.equalsIgnoreCase("application/octet-stream")){	//try to guess the content type
			    	InputStream is = new BufferedInputStream(new FileInputStream(fileContent));
			    	contentType = URLConnection.guessContentTypeFromStream(is);
			    	if (contentType==null || contentType.isEmpty()) contentType="application/octet-stream";
			    }
		    	ODocument doc=FileService.createFile(fileName,dataJson,contentType, fileContentAsByteArray);
		    	ret=prepareResponseToJson(doc);
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
				return ok(new ByteArrayInputStream(output));
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
>>>>>>> upstream/master
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result getFile(){
		  return status(NOT_IMPLEMENTED);
	  }
}
