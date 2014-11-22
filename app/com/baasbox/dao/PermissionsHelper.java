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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import play.Logger;

import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.BaasBoxException;
import com.baasbox.exception.AclNotValidException.Type;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
	public static PermissionJsonWrapper returnAcl(ObjectNode json,boolean check) throws AclNotValidException{
		return new PermissionJsonWrapper(json, check);
	}
	
	/***
	 * Set th ACL for a given document
	 * @param doc
	 * @param aclJson the json representing the acl to set.
	 * {"_allowRead":
	 *   [
	 *		{"name":"registered","isRole":true},
	 *		{"name":"johnny", "isRole":false}
	 *	 ]
	 * }
	 * Keys are: _allowRead,_allowWrite,_allowDelete,_allowUpdate
	 * @throws Exception
	 */
	public static void setAcl(ODocument doc, PermissionJsonWrapper acl)	throws AclNotValidException {
		if (acl.getAclJson()==null) return; 
		DbHelper.requestTransaction();
		try{
			revokeAll(doc);
			HashMap<Permissions,ArrayNode> hmp = new HashMap<Permissions,ArrayNode>();
			if (acl.getAllowRead()!=null) hmp.put(Permissions.ALLOW_READ, acl.getAllowRead());
			if (acl.getAllowUpdate()!=null) hmp.put(Permissions.ALLOW_UPDATE, acl.getAllowUpdate());
			if (acl.getAllowDelete()!=null) hmp.put(Permissions.ALLOW_DELETE, acl.getAllowDelete());
			
			Iterator<Entry<Permissions, ArrayNode>> itAllows = hmp.entrySet().iterator();
			while (itAllows.hasNext()){
				Entry<Permissions, ArrayNode> allows = itAllows.next();
				ArrayNode allow = allows.getValue();
				Permissions perm=allows.getKey();
				Iterator<JsonNode> itElements = allow.elements();
				while (itElements.hasNext()){
					JsonNode elem = itElements.next();
					if (!elem.isObject()) throw new AclNotValidException(Type.ACL_NOT_OBJECT, perm + " must contains array of objects");
					String name = ((ObjectNode)elem).get("name").asText();
					if (StringUtils.isEmpty(name)) throw new AclNotValidException(Type.ACL_KEY_NOT_VALID, "An element of the "+ perm + " field has no name");
					boolean isRole = isARole(elem);
					if (!isRole){
						grant(doc,perm,UserService.getOUserByUsername(name));
					}else grant(doc,perm,RoleService.getORole(name));
				}
			}
			DbHelper.commitTransaction();
		}catch (AclNotValidException e){
			DbHelper.rollbackTransaction();
			throw e;
		}catch (Exception e){
			DbHelper.rollbackTransaction();
			throw e;
		}
	}

	private static boolean isARole(JsonNode elem) {
		boolean isRole =  ((ObjectNode)elem).get("isrole")==null?false: ((ObjectNode)elem).get("isrole").asBoolean();
		if (!isRole) isRole =  ((ObjectNode)elem).get("isRole")==null?false: ((ObjectNode)elem).get("isRole").asBoolean();
		return isRole;
	}
	
	public static void revokeAll(ODocument doc){
		if (doc.field(Permissions.ALLOW_READ.toString())!=null)   ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_READ.toString()))).clear();
		if (doc.field(Permissions.ALLOW_DELETE.toString())!=null) ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_DELETE.toString()))).clear();
		if (doc.field(Permissions.ALLOW_UPDATE.toString())!=null) ((Set<OIdentifiable>) (doc.field(Permissions.ALLOW_UPDATE.toString()))).clear();
		if (doc.field(Permissions.FULL_ACCESS.toString())!=null)  ((Set<OIdentifiable>) (doc.field(Permissions.FULL_ACCESS.toString()))).clear();
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
