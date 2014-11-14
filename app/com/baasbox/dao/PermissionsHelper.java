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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import play.Logger;

import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.AclNotValidException.Type;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;



public class PermissionsHelper {

	public static final String ACL_FIELD=BaasBoxPrivateFields.ACL.toString();
	public static final String ACL_USERS_FIELD="users";
	public static final String ACL_ROLES_FIELD="roles";
	
	
	public static final Map<String, Permissions> permissionsFromString = ImmutableMap.of(
	        "read", Permissions.ALLOW_READ,
	        "update", Permissions.ALLOW_UPDATE,
	        "delete",Permissions.ALLOW_DELETE,
	        "all",Permissions.ALLOW
	); 
	
	/***
	 * return the ACL_FIELD from the given json object
	 *   the acl json must have the following format:
	 *	{
	 *		"read" : {
	 *			"users":[],
	 *			"roles":[]
	 *		}
	 *		"update" : .......
	 *	}
	 * @param json
	 * @param check if true, checks the correctness of the _acl content
	 * @return the acl
	 * @throws AclNotValidException 
	 */
	public static ObjectNode returnAcl(ObjectNode json,boolean check) throws AclNotValidException{
		JsonNode aclJson = json.get(ACL_FIELD);
		if (aclJson==null) return null;
		if (!aclJson.isObject()) throw new AclNotValidException(Type.ACL_NOT_OBJECT);
		if (check){
			/*check if the roles and users are valid*/
			 Iterator<Entry<String, JsonNode>> it = aclJson.fields();
			 while (it.hasNext()){
				 //check for permission read/update/delete/all
				 Entry<String, JsonNode> next = it.next();
				 if (!PermissionsHelper.permissionsFromString.containsKey(next.getKey())){
					 throw new AclNotValidException(Type.ACL_KEY_NOT_VALID,"The key '"+next.getKey()+"' is invalid. Valid ones are 'read','update','delete','all'");
				 }
				 //check for users/roles
				 Iterator<Entry<String, JsonNode>> it2 = next.getValue().fields();
				 while (it2.hasNext()){
					 Entry<String, JsonNode> next2 = it2.next();
					 if (!next2.getKey().equals("users") && !next2.getKey().equals("roles")) {
						 throw new AclNotValidException(Type.ACL_USER_OR_ROLE_KEY_UNKNOWN,"The key '"+ next2.getKey()+ "' is invalid. Valid ones are '"+ ACL_USERS_FIELD +"' or '"+ ACL_ROLES_FIELD +"'");
					 }
					 //check for the existence of users/roles
					 JsonNode arrNode = next2.getValue();
					 if (arrNode.isArray()) {
						    for (final JsonNode objNode : arrNode) {
						        //checks the existence users and/or roles
						    	if (next2.getKey().equals(ACL_USERS_FIELD) && !UserService.exists(objNode.asText())) 
						    		throw new AclNotValidException(Type.ACL_USER_DOES_NOT_EXIST,"The user " + objNode.asText() + " does not exists");
						    	if (next2.getKey().equals(ACL_ROLES_FIELD) && !RoleService.exists(objNode.asText())) 
						    		throw new AclNotValidException(Type.ACL_ROLE_DOES_NOT_EXIST,"The role " + objNode.asText() + " does not exists");
						    }
					 }else throw new AclNotValidException(Type.JSON_VALUE_MUST_BE_ARRAY,"The '"+next2.getKey()+"' value must be an array");
				 }
			 }
		}
		return (ObjectNode)aclJson;
	}
	
	public static void setAcl(ODocument doc, JsonNode aclJson)	throws Exception {
		if (aclJson==null) return; 
		Iterator<Entry<String, JsonNode>> itAction = aclJson.fields(); //read,update,delete
		DbHelper.requestTransaction();
		try{
			revokeAll(doc);
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
						    	   grant(doc,actionPermission,UserService.getOUserByUsername(element.asText()));
						       else 
						    	   grant(doc,actionPermission,RoleService.getORole(element.asText()));
						    }
					 }
				}
			}//set permissions
		}catch (Exception e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		DbHelper.commitTransaction();
	}
	
	public static void revokeAll(ODocument doc){
	     ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_READ.toString()))).clear();
	     ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_DELETE.toString()))).clear();
	     ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_UPDATE.toString()))).clear();
	     ((Set<OIdentifiable>) (doc.field(Permissions.FULL_ACCESS.toString()))).clear();
	     OUser author = UserService.getOUserByUsername(doc.field(BaasBoxPrivateFields.AUTHOR.toString()));
	     grant(doc,Permissions.FULL_ACCESS,author);
	}
	
	public static ODocument grantRead(ODocument document,ORole role){
		return grant(document,Permissions.ALLOW_READ,role);	
	}
	
	public static ODocument grantRead(ODocument document,OUser user){
		return grant(document,Permissions.ALLOW_READ,user);		
	}
	
	public static ODocument changeOwner(ODocument document,OUser user){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Set<ORID> set = new HashSet<ORID>();
		set.add( user.getDocument().getIdentity() ); 
		document.field( Permissions.ALLOW.toString(), set, OType.LINKSET ); 
		document.save(); 
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document;		
	}	
	
	public static ODocument changeOwner(ODocument document,OIdentifiable user){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Set<OIdentifiable> set = new HashSet<OIdentifiable>();
		set.add( user ); 
		document.field( Permissions.ALLOW.toString(), set, OType.LINKSET ); 
		document.save(); 
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document;		
	}

	
	public static ODocument grant(ODocument document, Permissions permission,
			ORole role) {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (role==null){
			Logger.warn("role is null! Grant command skipped");
			return document;
		}
		ODatabaseRecordTx db = DbHelper.getConnection();
		db.getMetadata().getSecurity().allowIdentity(document, permission.toString(), role.getDocument().getIdentity());
		document.save(); 
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document;
	}
	
	public static ODocument grant(ODocument document, Permissions permission,
			OUser user) {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		ODatabaseRecordTx db = DbHelper.getConnection();
		db.getMetadata().getSecurity().allowIdentity(document, permission.toString(), user.getDocument().getIdentity());
		document.save(); 
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document; 
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			ORole role) {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		ODatabaseRecordTx db = DbHelper.getConnection();
		db.getMetadata().getSecurity().disallowIdentity(document, permission.toString(), role.getDocument().getIdentity());
		document.save();
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document;
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			OUser user) {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		ODatabaseRecordTx db = DbHelper.getConnection();
		db.getMetadata().getSecurity().disallowIdentity(document, permission.toString(), user.getDocument().getIdentity());
		document.save(); 
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return document;
	}
}
