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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.exception.AssetNotFoundException;
import com.baasbox.service.storage.AssetService;
import com.baasbox.service.storage.FileService;
import com.baasbox.util.JSONFormats;
import com.google.common.io.Files;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;



public class File extends Controller {
	private static final String FILE_FIELD_NAME="file";
	private static final String DATA_FIELD_NAME="attachedData";
	
	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.FILE);
	}
	
	  /*------------------FILE--------------------*/
	@With ({UserOrAnonymousCredentialsFilter.class,ConnectToDBFilter.class})
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
	
	  public static Result getFileMetadata(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result getFileData(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result streamFile(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
	  public static Result updateData(){
		  return status(NOT_IMPLEMENTED);
	  }
	  
}
