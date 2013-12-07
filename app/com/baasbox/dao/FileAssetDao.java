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
package com.baasbox.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class FileAssetDao extends NodeDao {
	public final static String MODEL_NAME="_BB_FileAsset";
	public final static String BINARY_FIELD_NAME = "file";
	public final static String CONTENT_TYPE_FIELD_NAME="contentType";
	
	protected FileAssetDao(String modelName) {
		super(modelName);
	}
	
	public static FileAssetDao getInstance(){
		return new FileAssetDao(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String fileName, String contentType, byte[] content) instead");
	}
	
	public ODocument create(String name, String fileName, String contentType, byte[] content) throws Throwable{
		ODocument asset=super.create();
		ORecordBytes record = new ORecordBytes(content);
		asset.field(BINARY_FIELD_NAME,record);
		asset.field("name",name);
		asset.field("fileName",fileName);
		asset.field("contentType",contentType);
		asset.field("contentLength",content.length);
		super.grantPermission(asset, Permissions.ALLOW_READ,DefaultRoles.getORoles());
		super.grantPermission(asset, Permissions.ALLOW_UPDATE,DefaultRoles.getORoles()); //this is necessary due the resize API
		return asset;
	}
	
	public ODocument getByName (String name) throws SqlInjectionException{
		QueryParams criteria=QueryParams.getInstance().where("name=?").params(new String[]{name});
		List<ODocument> listOfAssets = this.get(criteria);
		if (listOfAssets==null || listOfAssets.size()==0) return null;
		return listOfAssets.get(0);
	}
	
	public ORecordBytes getBinary(ODocument doc) throws InvalidModelException{
		super.checkModelDocument(doc);
		return doc.field(BINARY_FIELD_NAME);
	}
	
	public byte[] getBinaryAsByte(ODocument doc) throws InvalidModelException{
			super.checkModelDocument(doc);
			ORecordBytes binary=doc.field(BINARY_FIELD_NAME);
			return binary.toStream();
	}

	public ODocument setBinary(ODocument doc, byte[] content) throws InvalidModelException {
		super.checkModelDocument(doc);
		ORecordBytes record = new ORecordBytes(content);
		doc.field(BINARY_FIELD_NAME,record);
		doc.field("contentLength",content.length);
		return doc;
	}

	public ODocument setContentType(ODocument doc, String contentType) throws InvalidModelException {
		super.checkModelDocument(doc);
		doc.field(CONTENT_TYPE_FIELD_NAME, contentType);
		return doc;
	}
	
	
	public  byte[] getStoredResizedPicture(ODocument asset, String sizePattern) throws InvalidModelException{
		super.checkModelDocument(asset);
		Map<String,ORID> resizedMap=(Map<String,ORID>) asset.field("resized");
		if (resizedMap!=null && resizedMap.containsKey(sizePattern)){
			ORecordBytes obytes = (ORecordBytes) resizedMap.get(sizePattern);
			return obytes.toStream();
		}
		return null;
	}
	
	public  void storeResizedPicture(ODocument asset,String sizePattern, byte[] resizedImage) throws InvalidModelException {
		super.checkModelDocument(asset);
		Map<String,ORID> resizedMap=(Map<String,ORID>) asset.field("resized");
		if (resizedMap==null) resizedMap=new HashMap<String,ORID>();
		resizedMap.put(sizePattern, new ORecordBytes().fromStream(resizedImage).save().getIdentity());
		asset.field("resized",resizedMap);
		this.save(asset);
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}	
}
