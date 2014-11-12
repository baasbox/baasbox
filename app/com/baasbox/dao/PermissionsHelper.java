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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import play.Logger;

import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;



public class PermissionsHelper {

	public static final Map<String, Permissions> permissionsFromString = ImmutableMap.of(
	        "read", Permissions.ALLOW_READ,
	        "update", Permissions.ALLOW_UPDATE,
	        "delete",Permissions.ALLOW_DELETE,
	        "all",Permissions.ALLOW
	); 
	
	public static ObjectNode extractAcl(ObjectNode json){
		/*extract acl*/
		/*the acl json must have the following format:
		 * {
		 * 		"read" : {
		 * 					"users":[],
		 * 					"roles":[]
		 * 				 }
		 * 		"update" : .......
		 * }
		 */
		
		String aclJsonString=null;
		if (acl!=null && datas.length>0){
			aclJsonString = acl[0];
			ObjectMapper mapper = new ObjectMapper();
			JsonNode aclJson=null;
			try{
				aclJson = mapper.readTree(aclJsonString);
			}catch(JsonProcessingException e){
				throw new AclNotValidException(CustomHttpCode.ACL_JSON_FIELD_MALFORMED.getBbCode(), "The 'acl' field is malformed", e);
			} catch (IOException e) {
				throw new AclNotValidException(CustomHttpCode.ACL_JSON_FIELD_MALFORMED.getBbCode(), "The 'acl' field is malformed", e);
			}
			/*check if the roles and users are valid*/
			 Iterator<Entry<String, JsonNode>> it = aclJson.fields();
			 while (it.hasNext()){
				 //check for permission read/update/delete/all
				 Entry<String, JsonNode> next = it.next();
				 if (!PermissionsHelper.permissionsFromString.containsKey(next.getKey())){
					 throw new AclNotValidException(CustomHttpCode.ACL_PERMISSION_UNKNOWN.getBbCode(),"The key '"+next.getKey()+"' is invalid. Valid ones are 'read','update','delete','all'");
				 }
				 //check for users/roles
				 Iterator<Entry<String, JsonNode>> it2 = next.getValue().fields();
				 while (it2.hasNext()){
					 Entry<String, JsonNode> next2 = it2.next();
					 if (!next2.getKey().equals("users") && !next2.getKey().equals("roles")) {
						 throw new AclNotValidException(CustomHttpCode.ACL_USER_OR_ROLE_KEY_UNKNOWN.getBbCode(),"The key '"+next2.getKey()+"' is invalid. Valid ones are 'users' or 'roles'");
					 }
					 //check for the existance of users/roles
					 JsonNode arrNode = next2.getValue();
					 if (arrNode.isArray()) {
						    for (final JsonNode objNode : arrNode) {
						        //checks the existance users and/or roles
						    	if (next2.getKey().equals("users") && !UserService.exists(objNode.asText())) 
						    		throw new AclNotValidException(CustomHttpCode.ACL_USER_DOES_NOT_EXIST.getBbCode(),"The user " + objNode.asText() + " does not exists");
						    	if (next2.getKey().equals("roles") && !RoleService.exists(objNode.asText())) 
						    		throw new AclNotValidException(CustomHttpCode.ACL_ROLE_DOES_NOT_EXIST.getBbCode(),"The role " + objNode.asText() + " does not exists");
						    	
						    }
					 }else throw new AclNotValidException(CustomHttpCode.JSON_VALUE_MUST_BE_ARRAY.getBbCode(),"The '"+next2.getKey()+"' value must be an array");
				 }
				 
			 }
			
		}else aclJsonString="{}";
		return aclJsonString;
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
