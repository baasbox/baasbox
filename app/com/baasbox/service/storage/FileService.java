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

package com.baasbox.service.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.dao.FileDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.AclNotValidException.Type;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.FileTooBigException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.storage.StorageUtils.ImageDimensions;
import com.baasbox.service.storage.StorageUtils.WritebleImageFormat;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class FileService {
	public static final String DATA_FIELD_NAME="attachedData";
	public static final String BINARY_FIELD_NAME=FileDao.BINARY_FIELD_NAME;
	public final static String CONTENT_TYPE_FIELD_NAME=FileDao.CONTENT_TYPE_FIELD_NAME;
	public final static String CONTENT_LENGTH_FIELD_NAME=FileDao.CONTENT_LENGTH_FIELD_NAME;
	
		public static ODocument createFile(String fileName,String data,String contentType, byte[] content) throws Throwable{
			InputStream is = new ByteArrayInputStream(content); 
			ODocument doc;
			try {
				doc=createFile(fileName,data,contentType, content.length , is);
			}finally {
				if (is != null)is.close();
			}
			return doc;
		}
		public static ODocument createFile(String fileName, String data,
				String contentType, long contentLength, InputStream is,
				HashMap<String,?> metadata, String contentString) throws Throwable {
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,contentLength,is,metadata,contentString);
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}
		
		public static ODocument createFile(String fileName, String dataJson,
				String aclJsonString, String contentType, long length,
				InputStream is, HashMap<String, Object> extractedMetaData,
				String extractedContent) throws Throwable {
			ODocument doc = createFile(fileName, dataJson, contentType, length, is, extractedMetaData,
					extractedContent);
			//sets the permissions
			ObjectMapper mapper = new ObjectMapper();
			JsonNode aclJson=null;
			try{
				aclJson = mapper.readTree(aclJsonString);
			}catch(JsonProcessingException e){
				throw e;
			}
			setAcl(doc, aclJson);
			return doc;
		}//createFile with permission
		
		private static void setAcl(ODocument doc, JsonNode aclJson)
				throws UserNotFoundException, RoleNotFoundException,
				FileNotFoundException, SqlInjectionException,
				InvalidModelException, AclNotValidException {
			Iterator<Entry<String, JsonNode>> itAction = aclJson.fields(); //read,update,delete
			while (itAction.hasNext()){
				Entry<String, JsonNode> nextAction = itAction.next();
				String action = nextAction.getKey();
				Permissions actionPermission = null;
				if (action.equalsIgnoreCase("read"))
					actionPermission=Permissions.ALLOW_READ;
				else if (action.equalsIgnoreCase("update"))
					actionPermission=Permissions.ALLOW_UPDATE;
				else if (action.equalsIgnoreCase("delete"))
					actionPermission=Permissions.ALLOW_DELETE;
				else if (action.equalsIgnoreCase("all"))
					actionPermission=Permissions.FULL_ACCESS;
				if (actionPermission==null) throw new AclNotValidException(Type.ACL_KEY_NOT_VALID, "'"+action+"' is not a valid permission to set. Allowed ones are: read, update, delete, all");
					
				Iterator<Entry<String, JsonNode>> itUsersRoles = nextAction.getValue().fields();

				while (itUsersRoles.hasNext()){
					 Entry<String, JsonNode> usersOrRoles = itUsersRoles.next();
					 JsonNode listOfElements = usersOrRoles.getValue();
					 if (listOfElements.isArray()) {
						    for (final JsonNode element : listOfElements) {
						       if (usersOrRoles.getKey().equalsIgnoreCase("users"))
						    	   grantPermissionToUser((String)doc.field("id"), actionPermission, element.asText());
						       else 
						    	   grantPermissionToRole((String)doc.field("id"), actionPermission, element.asText());
						    }
					 }
				}
			}//set permissions
		}
		
		
		public static ODocument createFile(String fileName,String data,String contentType, long contentLength , InputStream content) throws Throwable{
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,contentLength,content); 	
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}	
		
		public static ODocument createFile(String fileName,String data,String contentType, HashMap<String,?> metadata,long contentLength , InputStream content) throws Throwable{
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,contentLength,content); 
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}
		
		public static ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
			FileDao dao = FileDao.getInstance();
			return dao.getById(id);
		}
		
		public static void deleteById(String id) throws Throwable, SqlInjectionException, FileNotFoundException{
			FileDao dao = FileDao.getInstance();
			ODocument file=getById(id);
			if (file==null) throw new FileNotFoundException();
			dao.delete(file.getIdentity());
		}


		public static List<ODocument> getFiles(QueryParams criteria) throws SqlInjectionException {
			FileDao dao = FileDao.getInstance();
			return dao.get(criteria); 
		}


		public static ODocument grantPermissionToRole(String id,Permissions permission, String rolename) 
				throws RoleNotFoundException, FileNotFoundException, SqlInjectionException, InvalidModelException {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, role);
		}


		public static ODocument revokePermissionToRole(String id,
				Permissions permission, String rolename) throws RoleNotFoundException, FileNotFoundException, SqlInjectionException, InvalidModelException  {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, role);
		}	
		
		public static ODocument grantPermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException, InvalidModelException  {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, user);
		}

		public static ODocument revokePermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException, InvalidModelException {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, user);
		}
		
		/**
		 * 
		 * @param id
		 * @param width The desired width. It can be expressed both in pixel or in percentage (100px, or 20%)
		 * @param height The desired height. It can be expressed both in pixel or in percentage (100px, or 20%)
		 * @return
		 * @throws InvalidSizePatternException
		 * @throws SqlInjectionException
		 * @throws DocumentIsNotAnImageException
		 * @throws DocumentIsNotAFileException
		 * @throws DocumentNotFoundException
		 * @throws IOException
		 * @throws FileTooBigException 
		 */
		public static byte[] getResizedPicture(String id, String width, String height) throws InvalidSizePatternException, SqlInjectionException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException, FileTooBigException{
			String sizePattern = width + "-" + height;
			return getResizedPicture (id,sizePattern);
		}
		
		public static byte[] getResizedPicture(String id, int sizeId) throws InvalidSizePatternException, SqlInjectionException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException, FileTooBigException{
			String sizePattern = "";
			try{
				sizePattern=ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().split(" ")[sizeId];
			}catch (IndexOutOfBoundsException e){
				throw new InvalidSizePatternException("The specified id is out of range.");
			}
			return getResizedPicture (id,sizePattern);
		}
		
		public static byte[] getResizedPicture(String id, String sizePattern) throws InvalidSizePatternException, SqlInjectionException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException, FileTooBigException {
			ImageDimensions dimensions = StorageUtils.convertPatternToDimensions(sizePattern);
			return getResizedPicture (id,dimensions);
		}


		public static byte[] getResizedPicture(String id, ImageDimensions dimensions) throws SqlInjectionException, DocumentIsNotAnImageException, DocumentNotFoundException, DocumentIsNotAFileException, IOException, FileTooBigException {
			//get the file
			ODocument file;
			try {
				file = getById(id);
			} catch (InvalidModelException e1) {
				throw new DocumentIsNotAFileException("The id " + id + " is not a file");
			}
			if (file==null) throw new DocumentNotFoundException();
			//is the file an image?
			if (!StorageUtils.docIsAnImage(file)) throw new DocumentIsNotAnImageException("The file " + id + " is not an image");
			//are the dimensions allowed?
			//the check is delegated to the caller
			String sizePattern= dimensions.toString();
			try{
				FileDao dao=FileDao.getInstance();
				byte[] resizedImage = dao.getStoredResizedPicture( file,  sizePattern);
				if (resizedImage!=null) return resizedImage;
				
				ByteArrayOutputStream fileContent = StorageUtils.extractFileFromDoc(file);
				if (fileContent.toByteArray().length==0) return new byte[]{};
				String contentType = getContentType(file);
				String ext = contentType.substring(contentType.indexOf("/")+1);
				WritebleImageFormat format;
				try{
					format = WritebleImageFormat.valueOf(ext);
				}catch (Exception e){
					format= WritebleImageFormat.png;
				}
				resizedImage=StorageUtils.resizeImage(fileContent.toByteArray(), format, dimensions);
				
				//save the resized image for future requests
				dao.storeResizedPicture(file, sizePattern, resizedImage);
				return resizedImage;
			}catch ( InvalidModelException e) {
				throw new RuntimeException("A very strange error occurred! ",e);
			}catch (OutOfMemoryError e){
				throw new FileTooBigException();
			}

		}

		public static String getContentType(ODocument file) {
			return (String) file.field(CONTENT_TYPE_FIELD_NAME);
		}
		
		public static String getExtractedContent(String id) throws SqlInjectionException, InvalidModelException, FileNotFoundException {
			ODocument file = getById(id);
			if (file==null) throw new  FileNotFoundException();
			FileDao dao = FileDao.getInstance();
			String ret=dao.getExtractedContent(file);
			return ret;
		}




		
}
