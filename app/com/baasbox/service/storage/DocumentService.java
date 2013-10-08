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
import java.security.InvalidParameterException;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.baasbox.dao.DocumentDao;
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.NodeDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DocumentNotFoundException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.query.PartsLexer;
import com.baasbox.service.query.PartsLexer.Part;
import com.baasbox.service.query.PartsParser;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class DocumentService {


	public static final String FIELD_LINKS = NodeDao.FIELD_LINK_TO_VERTEX;

	public static ODocument create(String collection, JsonNode bodyJson) throws Throwable, InvalidCollectionException,InvalidModelException {
		DocumentDao dao = DocumentDao.getInstance(collection);

		ODocument doc = dao.create();
		dao.update(doc,(ODocument) (new ODocument()).fromJSON(bodyJson.toString()));

		PermissionsHelper.grantRead(doc, RoleDao.getFriendRole());	
		dao.save(doc);
		return doc;//.toJSON("fetchPlan:*:0 _audit:1,rid");
	}

	/**
	 * 
	 * @param collectionName
	 * @param rid
	 * @param bodyJson
	 * @return the updated document, null if the document is not found or belongs to another collection
	 * @throws InvalidCollectionException
	 * @throws DocumentNotFoundException 
	 * @throws IllegalArgumentException 
	 * @throws ODatabaseException 
	 */
	public static ODocument update(String collectionName,String rid, JsonNode bodyJson) throws InvalidCollectionException,InvalidModelException, ODatabaseException, IllegalArgumentException, DocumentNotFoundException {
		ODocument doc=get(collectionName,rid);
		if (doc==null) throw new InvalidParameterException(rid + " is not a valid document");
		//update the document
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		dao.update(doc,(ODocument) (new ODocument()).fromJSON(bodyJson.toString()));
		return doc;//.toJSON("fetchPlan:*:0 _audit:1,rid");
	}//update


	public static ODocument get(String collectionName,String rid) throws IllegalArgumentException,InvalidCollectionException,InvalidModelException, ODatabaseException, DocumentNotFoundException {
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		ODocument doc=dao.get(rid);
		return doc;
	}

	public static ODocument get(String collectionName,String rid,PartsParser parser) throws IllegalArgumentException,InvalidCollectionException,InvalidModelException, ODatabaseException, DocumentNotFoundException {
		DocumentDao dao = DocumentDao.getInstance(collectionName);
		ODocument doc=dao.get(rid);
		if(parser.isMultiField()){
			Object v = doc.field(parser.treeFields());
			
			if(v==null){
				throw new DocumentNotFoundException("Unable to find a field "+parser.treeFields()+" into document:"+rid);
			}
		}
		
		StringBuffer q = new StringBuffer();
		
		q.append("select ").append(parser.treeFields()).append(" from ").append(collectionName);
		q.append(" where @rid=").append(rid);
		ObjectMapper mp = new ObjectMapper();
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
					String json = String.format("{\"%s[%d]\":\"%s\"}",parser.last().getName(),af.arrayIndex,an.get(af.arrayIndex).getTextValue());
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
			JsonNode bodyJson, PartsParser pp) throws InvalidCollectionException,InvalidModelException, ODatabaseException, IllegalArgumentException, DocumentNotFoundException {
		ODocument od = get(rid);
		if (od==null) throw new InvalidParameterException(rid + " is not a valid document");
		if(pp.isMultiField()){
			Object v = od.field(pp.treeFields());

			if(v==null){
				throw new DocumentNotFoundException("Unable to find a field "+pp.treeFields()+" into document:"+rid);
			}
		}
		ObjectMapper mapper = new ObjectMapper();
		StringBuffer q = new StringBuffer("");

		//case 1 .coll (simple field) update <collectionName> set <field> = {json} where ...
		//case 2 .coll/.by (multi field) update <collectionName> merge {json} where ...
		//case 3 .coll/.arr[1] (multi field) modify array inline and fallback to case 2
		//case 4 .coll[x] like case 3
		if(!pp.isMultiField() && !pp.isArray()){
			q.append("update ").append(collectionName)
			.append(" set ")
			.append(pp.treeFields())
			.append(" = ")
			.append(bodyJson.get("data").toString());
		}else{
			q.append("update ").append(collectionName)
			.append(" merge ");
			try{
				String content = od.toJSON();
				ObjectNode json = (ObjectNode)mapper.readTree(content.toString());
				if(!(pp.last() instanceof PartsLexer.ArrayField)){
					json.put(pp.treeFields(), bodyJson.get("data"));
				}else if(pp.last() instanceof PartsLexer.ArrayField){
					PartsLexer.ArrayField arr = (PartsLexer.ArrayField)pp.last();
					JsonNode node = json;
					for (Part p : pp.parts()) {
						node = node.path(p.getName());
					}
					if(node.isArray()){
						ArrayNode arrNode = (ArrayNode)node;
						if(arrNode.size()<=arr.arrayIndex){
							arrNode.add(bodyJson.get("data"));
						}else{
							arrNode._set(arr.arrayIndex, bodyJson.get("data"));
						}
						json.put(pp.treeFields(), arrNode);
					}else{
						throw new InvalidModelException(pp.treeFields() + "is not an array");
					}

				}else{
					throw new InvalidModelException("Operation on arrays should provide an operation field: with add or remove value");
				}

			q.append(json.toString());
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Unable to modify inline json");
		}
	}
	q.append(" where @rid = ").append(rid);
	System.out.println("QUERY:"+q.toString());
	DocumentDao.getInstance(collectionName).updateByQuery(q.toString());
	od = get(rid);
	return od;
}
}
