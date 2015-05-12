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

import com.baasbox.controllers.actions.filters.*;
import com.baasbox.db.DbHelper;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;


import play.libs.F;
import com.baasbox.service.logging.BaasBoxLogger;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.AssetNotFoundException;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.OperationDisabledException;
import com.baasbox.service.storage.AssetService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.google.common.io.Files;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

public class Asset extends Controller{

	private static String prepareResponseToJson(ODocument doc){
		response().setContentType("application/json");
		doc.removeField("file");
		return JSONFormats.prepareResponseToJson(doc,JSONFormats.Formats.ASSET);
	}
	
	private static String prepareResponseToJson(List<ODocument> listOfDoc) throws IOException{
		response().setContentType("application/json");
		return  JSONFormats.prepareResponseToJson(listOfDoc,JSONFormats.Formats.ASSET);
	}
	
	
	
	//---------------------ACTIONS------------------------
	/**
	 * Returns the Asset. If the Asset is a file, returns its metadata
	 * @param name
	 * @return
	 * @throws InvalidModelException
	 */
	@With ({AnonymousCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> get(String name) throws InvalidModelException{
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(), () -> {
			ODocument doc = null;
			try {
				doc = AssetService.getByName(name);
			} catch (IllegalArgumentException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (InvalidModelException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;

			}
			if (doc == null) return notFound();
			String ret = prepareResponseToJson(doc);
			return ok(ret);
		}));
	}
	
	@With ({AnonymousCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> downloadResizedWH(String name,boolean forceDownload,String width, String height){
		//check if the automatic resize is allowed
		if (!ImagesConfiguration.IMAGE_ALLOWS_AUTOMATIC_RESIZE.getValueAsBoolean()) 
			return F.Promise.pure(badRequest("Image resizing was disabled by the administrator"));

		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try{
				ODocument doc=AssetService.getByName(name);
				if (doc==null || doc.field("file")==null) return notFound();
				byte[] output = AssetService.getResizedPicture(name, width, height);
				response().setContentType(AssetService.getContentType(doc));
				if(forceDownload) {
					String[] fileName=((String)doc.field("fileName")).split("\\.");
					String newFileName=fileName[0] + "_" + width + "-" + height + "." + (fileName.length>1?fileName[1]:"");
					response().setHeader("Content-Disposition", "attachment; filename=\""+newFileName+"\"");
				}
				response().setHeader("Content-Length", Long.toString(output.length));
				return ok(output);
			} catch (IllegalArgumentException | InvalidModelException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				BaasBoxLogger.error("error retrieving asset file content " + name, e);
				throw e;
			} catch (DocumentIsNotAnImageException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (DocumentIsNotAFileException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (InvalidSizePatternException e) {
				return badRequest("The requested resized dimensions are not allowed");
			}
		}));

	}
	
	@With ({AnonymousCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> downloadResizedInPerc(String name,boolean forceDownload,String dimensionsInPerc) {
		if (!dimensionsInPerc.endsWith("%")) {
			return F.Promise.pure(badRequest("The format must be a % (hint: put a % at the end of the url)"));
		}
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try{

				ODocument doc=AssetService.getByName(name);
				if (doc==null || doc.field("file")==null) return notFound();
				byte[] output = AssetService.getResizedPictureInPerc(name, dimensionsInPerc);
				response().setContentType(AssetService.getContentType(doc));
				if(forceDownload) {
					String[] fileName=((String)doc.field("fileName")).split("\\.");
					String newFileName=fileName[0] + "_" + dimensionsInPerc  + "%." + fileName[1];
					response().setHeader("Content-Disposition", "attachment; filename=\""+newFileName+"\"");
				}
				response().setHeader("Content-Length", Long.toString(output.length));
				return ok(output);
			} catch (IllegalArgumentException| InvalidModelException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				BaasBoxLogger.error("error retrieving asset file content " + name, e);
				throw e;
			} catch (DocumentIsNotAnImageException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (DocumentIsNotAFileException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (InvalidSizePatternException e) {
				return badRequest("The requested resized dimensions are not allowed");
			} catch (OperationDisabledException e) {
				return badRequest("The picture resize is disable");
			}
		}));

	}

	@With ({AnonymousCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> downloadSizeId(String name,boolean forceDownload,int sizeId ) {
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try{
				ODocument doc=AssetService.getByName(name);
				if (doc==null || doc.field("file")==null) return notFound();
				byte[] output = AssetService.getResizedPicture(name, sizeId);
				response().setContentType(AssetService.getContentType(doc));
				if(forceDownload) {
					String[] fileName=((String)doc.field("fileName")).split("\\.");
					String newFileName=fileName[0] + "_thumb_" + sizeId  + "." + fileName[1];
					response().setHeader("Content-Disposition", "attachment; filename=\""+newFileName+"\"");
				}
				response().setHeader("Content-Length", Long.toString(output.length));
				return ok(output);
			} catch (IllegalArgumentException|InvalidModelException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				BaasBoxLogger.error("error retrieving asset file content " + name, e);
				throw e;
			} catch (DocumentIsNotAnImageException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (DocumentIsNotAFileException e) {
				return badRequest("The requested asset is not an image and cannot be resized");
			} catch (InvalidSizePatternException e) {
				return badRequest("The requested resized dimensions are not allowed");
			} catch (OperationDisabledException e) {
				return badRequest("The picture resize is disable");
			}
		}));
	}

	/***
	 * Sends the requested file to the client.
	 * @param name the name of the asset
	 * @param forceDownload if true, force the UA to download the file adding the Content-Disposition header 
	 * @return
	 * @throws InvalidModelException
	 * @throws IOException
	 */
	@With ({AnonymousCredentialWrapFilterAsync.class, ConnectToDBFilterAsync.class})
	public static F.Promise<Result> download(String name,boolean forceDownload){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try {
				ODocument doc=AssetService.getByName(name);
				if (doc==null || doc.field("file")==null) return notFound();
				ORecordBytes record = doc.field("file");
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				record.toOutputStream(out);
				response().setContentType(AssetService.getContentType(doc));
				if(forceDownload) response().setHeader("Content-Disposition", "attachment; filename=\""+URLEncoder.encode((String)doc.field("fileName"),"UTF-8")+"\"");
				response().setHeader("Content-Length", ((Long)doc.field("contentLength")).toString());
				//return ok(new ByteArrayInputStream(out.toByteArray()));
				return ok(out.toByteArray());
			} catch (IllegalArgumentException | InvalidModelException e) {
				BaasBoxLogger.error("error retrieving asset " + name, e);
				throw e;
			} catch (SqlInjectionException e) {
				return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
			} catch (IOException e) {
				BaasBoxLogger.error("error retrieving asset file content " + name, e);
				throw e;
			}
		}));
	}

	@With  ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class, CheckAdminRoleFilterAsync.class, ExtractQueryParameters.class})
	public static F.Promise<Result> getAll() throws  Throwable{
		Context ctx=Http.Context.current.get();

		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);
		return F.Promise.promise(DbHelper.withDbFromContext(ctx, () -> {
			List<ODocument> listOfDocs = AssetService.getAssets(criteria);
			return ok(prepareResponseToJson(listOfDocs));
		}));
	}
	
	
	private static Result postFile() throws  Throwable{
		MultipartFormData  body = request().body().asMultipartFormData();
		if (body==null) return badRequest("missing data: is the body multipart/form-data? Check if it contains boundaries too!");
		FilePart file = body.getFile("file");
		Map<String, String[]> data=body.asFormUrlEncoded();
		String[] meta=data.get("meta");
		String[] name=data.get("name");
		if (name==null || name.length==0 || StringUtils.isEmpty(name[0].trim())) return badRequest("missing name field");
		String ret="";
		if (file!=null){
			String metaJson=null;
			if (meta!=null && meta.length>0){
				metaJson = meta[0];
			}
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
		    	ODocument doc=AssetService.createFile(name[0],fileName,metaJson,contentType, fileContentAsByteArray);
		    	ret=prepareResponseToJson(doc);
		    }catch (ORecordDuplicatedException e){
		    	return badRequest("An asset with the same name already exists");
		    }catch (OIndexException e){
		    	return badRequest("An asset with the same name already exists");
		    }
		}else{
			return badRequest("missing file field");
		}
	  return created(ret);
	}

	@With  ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class, CheckAdminRoleFilterAsync.class})
	public static F.Promise<Result> post() throws  Throwable{
		String ct = request().getHeader(CONTENT_TYPE);
		if (ct.contains("multipart/form-data")) {
			return F.Promise.promise(DbHelper.withDbFromContext(ctx(),Asset::postFile));
		}
		Map<String, String[]> body = request().body().asFormUrlEncoded();
		if (body==null) {
			return F.Promise.pure(badRequest("missing data: is the body x-www-form-urlencoded?"));
		}
		String[] meta=body.get("meta");
		String[] name=body.get("name");
		String assetName = (name!=null && name.length>0) ? name[0] : null;
		if (assetName!=null&&StringUtils.isNotEmpty(assetName.trim())){
			return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
				String ret="";
					String metaJson=null;
					if (meta!=null && meta.length>0){
						metaJson = meta[0];
					}
					try{
						ODocument doc=AssetService.create(assetName,metaJson);
						ret=prepareResponseToJson(doc);
					}catch (ORecordDuplicatedException e){
						return badRequest("An asset with the same name already exists");
					}catch (OIndexException e){
						return badRequest("An asset with the same name already exists");
					}catch (InvalidJsonException e){
						return badRequest("the meta field has a problem: " + e.getMessage());
					}

				return created(ret);
			}));
		}else{
			return F.Promise.pure(badRequest("missing name field"));
		}


	}
	
	@With  ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class, CheckAdminRoleFilterAsync.class})
	public static F.Promise<Result> delete(String name){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(), () -> {
			try {
				AssetService.deleteByName(name);
			} catch (AssetNotFoundException e) {
				return notFound(name + " not found");
			}
			return ok();
		}));
	}
}
