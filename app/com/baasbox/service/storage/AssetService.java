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
package com.baasbox.service.storage;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.dao.AssetDao;
import com.baasbox.dao.FileAssetDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.AssetNotFoundException;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.OperationDisabledException;
import com.baasbox.service.storage.StorageUtils.ImageDimensions;
import com.baasbox.service.storage.StorageUtils.WritebleImageFormat;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class AssetService {
	public static ODocument create(String name, String meta) throws InvalidJsonException,Throwable{
		AssetDao dao = AssetDao.getInstance();
		ODocument doc=dao.create(name);
		try{
			
			if (meta!=null && !meta.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ 'meta' : " + meta + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
		}catch (OSerializationException e){
			throw new InvalidJsonException(e);
		}catch (Throwable e){
			throw e;
		}
		return doc;
	}
	
	public static ODocument createFile(String name,String fileName,String meta,String contentType, byte[] content) throws Throwable{
		FileAssetDao dao = FileAssetDao.getInstance();
		ODocument doc=dao.create(name,fileName,contentType,content);
		if (meta!=null && !meta.trim().isEmpty()) {
			ODocument metaDoc=(new ODocument()).fromJSON("{ 'meta' : " + meta + "}");
			doc.merge(metaDoc, true, false);
		}
		dao.save(doc);
		return doc;
	}
	
	public static ODocument get(String rid) throws SqlInjectionException, IllegalArgumentException, InvalidModelException, ODatabaseException, DocumentNotFoundException {
		AssetDao dao = AssetDao.getInstance();
		return dao.get(rid);
	}
	
	public static ODocument getByName(String name) throws SqlInjectionException, IllegalArgumentException, InvalidModelException {
		AssetDao dao = AssetDao.getInstance();
		return dao.getByName(name);
	}
	

	
	public static ByteArrayOutputStream getFileAsStream (String fileAssetName) throws SqlInjectionException, IOException{
		FileAssetDao dao = FileAssetDao.getInstance();
		ODocument fileAsset=dao.getByName(fileAssetName);
		if (fileAsset==null) return null;
		ORecordBytes record = fileAsset.field("file");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		record.toOutputStream(out);
		return out;
	}
	
	public static ByteArrayOutputStream getPictureAsStream (String fileAssetName) throws SqlInjectionException, IOException{
		return getFileAsStream (fileAssetName);
	}
	
	public static byte[] getResizedPicture (String fileAssetName, String width, String height) throws SqlInjectionException, IOException, DocumentIsNotAnImageException, DocumentIsNotAFileException, InvalidSizePatternException{
		String sizePattern = width + "-" + height;
		return getResizedPicture (fileAssetName,sizePattern);
	}
	
	public static byte[] getResizedPictureInPerc (String fileAssetName, String sizeInPerc) throws SqlInjectionException, IOException, DocumentIsNotAnImageException, DocumentIsNotAFileException, InvalidSizePatternException, OperationDisabledException{
		if (!sizeInPerc.endsWith("%")) throw new InvalidSizePatternException();
		return getResizedPicture (fileAssetName,sizeInPerc);
	}
	
	public static byte[] getResizedPicture (String fileAssetName, int sizeId) throws SqlInjectionException, IOException, InvalidSizePatternException, DocumentIsNotAnImageException, DocumentIsNotAFileException, OperationDisabledException{
		String sizePattern = "";
		try{
			sizePattern=ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().split(" ")[sizeId];
		}catch (IndexOutOfBoundsException e){
			throw new InvalidSizePatternException("The specified id is out of range.");
		}
		return getResizedPicture (fileAssetName,sizePattern);
	}
	
	public static byte[] getResizedPicture (String fileAssetName, String sizePattern) throws SqlInjectionException, IOException, InvalidSizePatternException, DocumentIsNotAnImageException, DocumentIsNotAFileException{
		if (!ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().contains(sizePattern))
			throw new InvalidSizePatternException("The requested resize format is not allowed");
		ImageDimensions dimensions = StorageUtils.convertPatternToDimensions(sizePattern);
		return getResizedPicture (fileAssetName,dimensions);
	}

	
	private static byte[] getResizedPicture(String fileAssetName, ImageDimensions dimensions) throws SqlInjectionException, DocumentIsNotAnImageException, DocumentIsNotAFileException, IOException, InvalidSizePatternException {
		//load the document
		FileAssetDao dao = FileAssetDao.getInstance();
		ODocument asset=dao.getByName(fileAssetName);
		if (asset==null) return null;
		if (!StorageUtils.docIsAnImage(asset)) throw new DocumentIsNotAnImageException();
		
		//check if the image has been previously resized
		String sizePattern= dimensions.toString();
		try{
			byte[] resizedImage = dao.getStoredResizedPicture( asset,  sizePattern);
			if (resizedImage!=null) return resizedImage;
			
			ByteArrayOutputStream fileContent = StorageUtils.extractFileFromDoc(asset);
			String contentType = getContentType(asset);
			String ext = contentType.substring(contentType.indexOf("/")+1);
			WritebleImageFormat format;
			try{
				format = WritebleImageFormat.valueOf(ext);
			}catch (Exception e){
				format= WritebleImageFormat.png;
			}
			resizedImage=StorageUtils.resizeImage(fileContent.toByteArray(), format, dimensions);
			
			//save the resized image for future requests
			dao.storeResizedPicture(asset, sizePattern, resizedImage);
			return resizedImage;
		}catch ( InvalidModelException e) {
			throw new RuntimeException("A very strange error occurred! ",e);
		}
	}



	public static String getContentType(ODocument asset) {
		return (String) asset.field("contentType");
	}

	public static long getCount(QueryParams criteria) throws InvalidCollectionException, SqlInjectionException{
		AssetDao dao = AssetDao.getInstance();
		return dao.getCount(criteria);
	}
	
	public static List<ODocument> getAssets(QueryParams criteria) throws SqlInjectionException{
		AssetDao dao = AssetDao.getInstance();
		return dao.get(criteria);
	}
	
	public static void deleteByRid(String rid) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		dao.delete(rid);
	}
	public static void deleteByRid(ORID rid) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		dao.delete(rid);
	}	
	public static void deleteByName(String name) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		ODocument asset=getByName(name);
		if (asset==null) throw new AssetNotFoundException();
		dao.delete(asset.getIdentity());
	}	
}
