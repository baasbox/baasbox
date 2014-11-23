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

import java.security.InvalidParameterException;
import java.util.List;

import com.baasbox.controllers.actions.exceptions.RidNotFoundException;
import com.baasbox.dao.DocumentDao;
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.PermissionJsonWrapper;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UpdateOldVersionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.query.JsonTree;
import com.baasbox.service.query.MissingNodeException;
import com.baasbox.service.query.PartsParser;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import play.Logger;


public class DocumentService {


	public static final String FIELD_LINKS = NodeDao.FIELD_LINK_TO_VERTEX;
	private static final String OBJECT_QUERY_ALIAS = "result";

	public static ODocument create(String collection, ObjectNode bodyJson) throws Throwable, InvalidCollectionException,InvalidModelException {
		DocumentDao dao = DocumentDao.getInstance(collection);
		DbHelper.requestTransaction();
		ODocument doc = null;
		try	{
			doc = dao.create();
			PermissionJsonWrapper acl = PermissionsHelper.returnAcl(bodyJson, true);
			dao.update(doc,(ODocument) (new ODocument()).fromJSON(bodyJson.toString()));
			PermissionsHelper.setAcl(doc, acl);
			dao.save(doc);
			DbHelper.commitTransaction();
		}catch (OSerializationException e){
			DbHelper.rollbackTransaction();
			throw new InvalidJsonException(e);
		}catch (UpdateOldVersionException e){
			DbHelper.rollbackTransaction();
			throw new UpdateOldVersionException("Are you trying to create a document with a @version field?");
		}catch(AclNotValidException e){
			DbHelper.rollbackTransaction();
			throw e;
		}catch (Exception e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		return doc;
	}

	/**
	 * @throws DocumentNotFoundException 
	 * @throws InvalidModelException 
	 * @throws InvalidCollectionException 
	 * @throws IllegalArgumentException 
	 * @throws ODatabaseException 
	 * @throws UpdateOldVersionException
	 * @throws AclNotValidException
	 * 
	 * @param collectionName
	 * @param rid
	 * @param bodyJson
	 * @return the updated document, null if the document is not found or belongs to another collection
	 * @throws  
	 */
	public static ODocument update(String collectionName,String rid, ObjectNode bodyJson) throws AclNotValidException,ODatabaseException, IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException ,UpdateOldVersionException  {
		ODocument doc=get(collectionName,rid);
		if (doc==null) throw new InvalidParameterException(rid + " is not a valid document");
		//update the document
		DbHelper.requestTransaction();
		try{
			DocumentDao dao = DocumentDao.getInstance(collectionName);
			PermissionJsonWrapper acl = PermissionsHelper.returnAcl(bodyJson, true);
			dao.update(doc,(ODocument) (new ODocument()).fromJSON(bodyJson.toString()));
			PermissionsHelper.setAcl(doc, acl);
			DbHelper.commitTransaction();
		}catch(AclNotValidException | UpdateOldVersionException | InvalidCollectionException e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		return doc;
	}//update


	public static ODocument get(String collectionName,String rid) throws IllegalArgumentException,InvalidCollectionException,InvalidModelException, ODatabaseException, DocumentNotFoundException {
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		ODocument doc=dao.get(rid);

		return doc;
	}


	public static ODocument get(String collectionName,String rid,PartsParser parser) throws IllegalArgumentException,InvalidCollectionException,InvalidModelException, ODatabaseException, DocumentNotFoundException, InvalidCriteriaException {
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		ODocument doc=dao.get(rid);
		if(parser.isMultiField()){
			Object v = doc.field(parser.treeFields());

			if(v==null){
				throw new DocumentNotFoundException("Unable to find a field "+parser.treeFields()+" into document:"+rid);
			}
		}

		StringBuffer q = new StringBuffer();
		q.append("select ")
		.append(parser.fullTreeFields())
		.append(" as ").append(OBJECT_QUERY_ALIAS)
		.append(" from ").append(rid);
		List<ODocument> odocs = DocumentDao.getInstance(collectionName).selectByQuery(q.toString());
		ODocument result = (odocs!=null && !odocs.isEmpty())?odocs.iterator().next():null;

		//TODO:
		/*if(parser.isArray()){
			try {
				ArrayNode an = (ArrayNode)mp.readTree(result.toJSON()).get(parser.last().getName());
				PartsLexer.ArrayField af =  (PartsLexer.ArrayField)parser.last();
				if(an.size()<af.arrayIndex){
					throw new InvalidModelException("The index requested does not exists in model");
				}else{
					String json = String.format("{\"%s[%d]\":\"%s\"}",parser.last().getName(),af.arrayIndex,an.get(af.arrayIndex).textValue());
					result = new ODocument().fromJSON(json);
					System.out.println("JSON:"+result.toJSON());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/

		return result;
	}


	public static long getCount(String collectionName, QueryParams criteria) throws InvalidCollectionException, SqlInjectionException{
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		return dao.getCount(criteria);
	}

	public static List<ODocument> getDocuments(String collectionName, QueryParams criteria) throws SqlInjectionException, InvalidCollectionException{
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		return dao.get(criteria);
	}

	/**
	 * @param rid
	 * @return
	 */
	public static ODocument get(String rid) {
		GenericDao dao = GenericDao.getInstance();
		ODocument doc=dao.get(rid);
     	return doc;
	}

	/**
	 * @param collectionName
	 * @param rid
	 * @throws Throwable 
	 */
	public static void delete(String collectionName, String rid) throws Throwable {
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		try {
			dao.delete(rid);
		} catch (InvalidModelException e) {
			throw new InvalidCollectionException("The document " + rid + " does no belong to the collection " + collectionName);
		}
	}

	public static ODocument grantPermissionToUser(String collectionName, String rid, Permissions permission, String username) throws UserNotFoundException, IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException {
		OUser user=UserService.getOUserByUsername(username);
		if (user==null) throw new UserNotFoundException(username);
		ODocument doc = get(collectionName, rid);
		if (doc==null) throw new DocumentNotFoundException(rid);
		return PermissionsHelper.grant(doc, permission, user);
	}

	public static ODocument revokePermissionToUser(String collectionName, String rid, Permissions permission, String username) throws UserNotFoundException, IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException {
		OUser user=UserService.getOUserByUsername(username);
		if (user==null) throw new UserNotFoundException(username);
		ODocument doc = get(collectionName, rid);
		if (doc==null) throw new DocumentNotFoundException(rid);
		return PermissionsHelper.revoke(doc, permission, user);
	}

	public static ODocument grantPermissionToRole(String collectionName, String rid, Permissions permission, String rolename) throws RoleNotFoundException, IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException {
		ORole role=RoleDao.getRole(rolename);
		if (role==null) throw new RoleNotFoundException(rolename);
		ODocument doc = get(collectionName, rid);
		if (doc==null) throw new DocumentNotFoundException(rid);
        return PermissionsHelper.grant(doc, permission, role);
	}

	public static ODocument revokePermissionToRole(String collectionName, String rid, Permissions permission, String rolename) throws  IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException, RoleNotFoundException {
		ORole role=RoleDao.getRole(rolename);
		if (role==null) throw new RoleNotFoundException(rolename);
		ODocument doc = get(collectionName, rid);
		if (doc==null) throw new DocumentNotFoundException(rid);
		return PermissionsHelper.revoke(doc, permission, role);
	}
	public static ODocument update(String collectionName, String rid,
			ObjectNode bodyJson, PartsParser pp) throws MissingNodeException, InvalidCollectionException,InvalidModelException, ODatabaseException, IllegalArgumentException, DocumentNotFoundException {
		ODocument od = get(rid);
		if (od==null) throw new InvalidParameterException(rid + " is not a valid document");
		ObjectMapper mapper = new ObjectMapper();
		StringBuffer q = new StringBuffer("");

		if(!pp.isMultiField() && !pp.isArray()){
			q.append("update ").append(rid)
			.append(" set ")
			.append(pp.treeFields())
			.append(" = ")
			.append(bodyJson.get("data").toString());

		}else{
			q.append("update ").append(rid)
			.append(" merge ");
			String content = od.toJSON();
			ObjectNode json = null;
			try{
				json = (ObjectNode)mapper.readTree(content.toString());
			}catch(Exception e){
				throw new RuntimeException("Unable to modify inline json");
			}
			JsonTree.write(json, pp, bodyJson.get("data"));
			q.append(json.toString());
		}
		try{
			DocumentDao.getInstance(collectionName).updateByQuery(q.toString());
		}catch(OSecurityException  e){
			throw e;
		} catch (InvalidCriteriaException e) {
			throw new RuntimeException (e);
		}
		od = get(collectionName,rid);
		return od;
	}
	
	public static ODocument setAcl(String collection, String uuid, PermissionJsonWrapper acl) throws ODatabaseException, IllegalArgumentException, InvalidCollectionException, InvalidModelException, DocumentNotFoundException, AclNotValidException{
		if (acl.getAclJson()==null)  acl.empty(); //force permission to nobody if no acl ha been set at all
		GenericDao gdao = GenericDao.getInstance();
		ORID rid=gdao.getRidNodeByUUID(uuid);
		ODocument doc = get(collection,rid.toString());
		PermissionsHelper.setAcl(doc, acl);
		return doc;
	}

    public static String getRidByString(String id, boolean isUUID) throws RidNotFoundException{
        String rid = null;
        if (isUUID) {
            if (Logger.isDebugEnabled()) Logger.debug("id is an UUID, try to get a valid RID");
            ORID orid = GenericDao.getInstance().getRidNodeByUUID(id);
            if (orid == null) throw new RidNotFoundException(id);
            rid = orid.toString();
            if (Logger.isDebugEnabled()) Logger.debug("Retrieved RID: "+ rid);
        } else {
            rid = "#"+id;
        }
        return rid;
    }

}
