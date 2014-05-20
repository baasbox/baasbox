/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class FileDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_File";
	public final static String BINARY_FIELD_NAME = "file";
	public final static String CONTENT_TYPE_FIELD_NAME="contentType";
	public final static String CONTENT_LENGTH_FIELD_NAME="contentLength";
	public static final String FILENAME_FIELD_NAME="fileName";
	private static final String RESIZED_IMAGE_FIELD_NAME="resized";
	public static final String METADATA_FIELD_NAME = "metadata";
	private static final String FILE_CONTENT_CLASS = "_BB_FILE_CONTENT";
	public static final String FILE_CONTENT_FIELD_NAME = "text_content";
	
	protected FileDao(String modelName) {
		super(modelName);
	}
	
	public static FileDao getInstance(){
		return new FileDao(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String fileName, String contentType, byte[] content) instead");
	}
	
	public ODocument create(String fileName, String contentType, byte[] content) throws Throwable{
		ODocument file=super.create();
		ORecordBytes record = new ORecordBytes(content);
		file.field(BINARY_FIELD_NAME,record);
		file.field(FILENAME_FIELD_NAME,fileName);
		file.field(CONTENT_TYPE_FIELD_NAME,contentType);
		file.field(CONTENT_LENGTH_FIELD_NAME,new Long(content.length));
		return file;
	}

	public ODocument create(String fileName, String contentType, long contentLength, InputStream content) throws Throwable{
		return this.create(fileName, contentType, contentLength, content, null, null);
	}
	
	public ODocument create(String fileName, String contentType,
			long contentLength, InputStream is, HashMap<String, ?> metadata,
			String contentString) throws Throwable {
		ODocument file=super.create();
		ORecordBytes record = new ORecordBytes();
		record.fromInputStream(is, (int) contentLength);
		file.field(BINARY_FIELD_NAME,record);
		file.field(FILENAME_FIELD_NAME,fileName);
		file.field(CONTENT_TYPE_FIELD_NAME,contentType);
		file.field(CONTENT_LENGTH_FIELD_NAME,new Long(contentLength));
		if (metadata!=null){
            ODocument doc = new ODocument();
            doc = doc.fromJSON(new JSONObject(metadata).toString());
			file.field(METADATA_FIELD_NAME,doc);
		}
		if (!StringUtils.isEmpty(contentString)){
			file.field(FILE_CONTENT_FIELD_NAME,(new ODocument(FILE_CONTENT_CLASS)).field("content",contentString));
		}
		return file;
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

	public ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria=QueryParams.getInstance().where("id=?").params(new String[]{id});
		List<ODocument> listOfFiles = this.get(criteria);
		if (listOfFiles==null || listOfFiles.size()==0) return null;
		ODocument doc=listOfFiles.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the id may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the id " + id + " is not a file " + this.MODEL_NAME);
		}
		return doc;
	}	
	
	public  byte[] getStoredResizedPicture(ODocument file, String sizePattern) throws InvalidModelException{
		super.checkModelDocument(file);
		Map<String,ORID> resizedMap=(Map<String,ORID>) file.field(RESIZED_IMAGE_FIELD_NAME);
		if (resizedMap!=null && resizedMap.containsKey(sizePattern)){
			ORecordBytes obytes = (ORecordBytes) resizedMap.get(sizePattern);
			return obytes.toStream();
		}
		return null;
	}
	
	public  void storeResizedPicture(ODocument file,String sizePattern, byte[] resizedImage) throws InvalidModelException {
		super.checkModelDocument(file);
		Map<String,ORID> resizedMap=(Map<String,ORID>) file.field(RESIZED_IMAGE_FIELD_NAME);
		if (resizedMap==null) resizedMap=new HashMap<String,ORID>();
		resizedMap.put(sizePattern, new ORecordBytes().fromStream(resizedImage).save().getIdentity());
		file.field(RESIZED_IMAGE_FIELD_NAME,resizedMap);
		try{
			this.save(file);
		}catch (OConcurrentModificationException e){
			//just ignore it...
		}catch (OSecurityException e){ 
			//just ignore it because it happens when someone who has read access to the file, but not the right to update it, are asking for it 
		}
		
	}

	public String getExtractedContent(ODocument file) { 
		ODocument extractedContentDocument=file.field(FILE_CONTENT_FIELD_NAME);
		if (extractedContentDocument==null) return  "";
		String content=extractedContentDocument.field("content");
		return  content;
	}



}
