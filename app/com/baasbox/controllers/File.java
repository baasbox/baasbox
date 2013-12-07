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

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.service.storage.FileService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.google.common.io.Files;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;



public class File extends Controller {
	private static final String FILE_FIELD_NAME="file";
	private static final String DATA_FIELD_NAME="attachedData";
	
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
			if (body==null) return badRequest("missing data: is the body multipart/form-data?");
			FilePart file = body.getFile(FILE_FIELD_NAME);
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
			    try{
			    	ODocument doc=FileService.createFile(fileName,dataJson,contentType, fileContentAsByteArray);
			    	ret=prepareResponseToJson(doc);
			    }catch (ORecordDuplicatedException e){
			    	return badRequest("An file with the same name already exists");
			    }catch (OIndexException e){
			    	return badRequest("An file with the same name already exists");
			    }
			}else{
				return badRequest("missing '"+FILE_FIELD_NAME+"' field");
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
		}
		if (doc==null) return notFound(id + " file was not found");
		return ok(prepareResponseToJson(doc));
	}
	
		@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class})
	  public static Result streamFile(String id) throws IOException{
			try {
				ODocument doc=FileService.getById(id);
				if (doc==null) return notFound(id + " file was not found");
				ORecordBytes record = doc.field(FileService.BINARY_FIELD_NAME);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				record.toOutputStream(out);
				response().setContentType((String)doc.field(FileService.CONTENT_TYPE_FIELD_NAME));
				response().setHeader("Content-Length", ((Long)doc.field(FileService.CONTENT_LENGTH_FIELD_NAME)).toString());
				return ok(new ByteArrayInputStream(out.toByteArray()));
			} catch (SqlInjectionException e) {
				return badRequest("the supplied id appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				Logger.error("error retrieving file content " + id, e);
				throw e;
			}
	  }//streamFile
	  
		@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class})
	  public static Result downloadFile(String id) throws IOException{
		   try {
				ODocument doc=FileService.getById(id);
				if (doc==null) return notFound(id + " file was not found");
				ORecordBytes record = doc.field(FileService.BINARY_FIELD_NAME);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				record.toOutputStream(out);
				response().setContentType((String)doc.field(FileService.CONTENT_TYPE_FIELD_NAME));
				response().setHeader("Content-Length", ((Long)doc.field(FileService.CONTENT_LENGTH_FIELD_NAME)).toString());
				response().setHeader("Content-Disposition", "attachment; filename=\""+URLEncoder.encode((String)doc.field("fileName"),"UTF-8")+"\"");
				return ok(new ByteArrayInputStream(out.toByteArray()));
			} catch (SqlInjectionException e) {
				return badRequest("the supplied id appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				Logger.error("error retrieving file content " + id, e);
				throw e;
			}		  
	  }//downloadFile
	  
	
		
	  public static Result updateAttachedData(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
}
