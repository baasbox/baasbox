package com.baasbox.service.storage;

import java.util.List;

import com.baasbox.dao.FileDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
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
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,content);
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}

		
		public static ODocument getById(String id) throws SqlInjectionException {
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


		public static ODocument grantPermissionToRole(String id,Permissions permission, String rolename) throws RoleNotFoundException, FileNotFoundException, SqlInjectionException {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, role);
		}


		public static ODocument revokePermissionToRole(String id,
				Permissions permission, String rolename) throws RoleNotFoundException, FileNotFoundException, SqlInjectionException  {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, role);
		}	
		
		public static ODocument grantPermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException  {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, user);
		}

		public static ODocument revokePermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, user);
		}
}
