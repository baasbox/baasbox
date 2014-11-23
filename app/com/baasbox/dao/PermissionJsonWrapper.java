package com.baasbox.dao;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.AclNotValidException.Type;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PermissionJsonWrapper {
	private ObjectNode aclJson;
	
	public ObjectNode getAclJson() {
		return aclJson;
	}
	
	public ArrayNode getAllowRead() {
		return (ArrayNode) aclJson.get(BaasBoxPrivateFields.ALLOW_READ.toString());
	}
	public ArrayNode getAllowUpdate() {
		return (ArrayNode) aclJson.get(BaasBoxPrivateFields.ALLOW_UPDATE.toString());
	}
	public ArrayNode getAllowDelete() {
		return (ArrayNode) aclJson.get(BaasBoxPrivateFields.ALLOW_DELETE.toString());
	}

	
	public PermissionJsonWrapper(ObjectNode json,boolean check) throws AclNotValidException {
		
		ArrayNode allowRead;
		ArrayNode allowUpdate;
		ArrayNode allowDelete;
		
		try{
			allowRead=(ArrayNode) json.get(BaasBoxPrivateFields.ALLOW_READ.toString());
			allowUpdate=(ArrayNode) json.get(BaasBoxPrivateFields.ALLOW_UPDATE.toString());
			allowDelete=(ArrayNode) json.get(BaasBoxPrivateFields.ALLOW_DELETE.toString());
		}catch(ClassCastException e){
			throw new AclNotValidException(Type.JSON_VALUE_MUST_BE_ARRAY,"The " +
				 BaasBoxPrivateFields.ALLOW_READ.toString() + ", " +
				 BaasBoxPrivateFields.ALLOW_UPDATE.toString() + ", " +
				 BaasBoxPrivateFields.ALLOW_DELETE.toString() + ", " +
				 "values must be an array");
		}
		
		if (allowRead==null && allowUpdate==null && allowDelete==null) 
			this.aclJson=null;
		else {
			ObjectNode aclJson=new ObjectNode(JsonNodeFactory.instance);
			if (allowRead!=null) aclJson.set(BaasBoxPrivateFields.ALLOW_READ.toString(), allowRead);
			if (allowUpdate!=null) aclJson.set(BaasBoxPrivateFields.ALLOW_UPDATE.toString(), allowUpdate);
			if (allowDelete!=null) aclJson.set(BaasBoxPrivateFields.ALLOW_DELETE.toString(), allowDelete);
			this.aclJson=aclJson;
			
			if (check){
				check();
			}
			
		}
	}

	public void check() throws AclNotValidException {
		if (this.aclJson==null) return;
		//cycle through the _allow* fields
		Iterator<Entry<String, JsonNode>> itAllows = aclJson.fields();
		while (itAllows.hasNext()){
			Entry<String, JsonNode> allow = itAllows.next();
			ArrayNode elementsToCheck = (ArrayNode) allow.getValue();
			Iterator<JsonNode> itElements = elementsToCheck.elements();
			while (itElements.hasNext()){
				JsonNode elemToCheck = itElements.next();
				if (!elemToCheck.isObject()) throw new AclNotValidException(Type.ACL_NOT_OBJECT, allow.getKey() + " must contains array of objects");
				String name = ((ObjectNode)elemToCheck).get("name").asText();
				if (StringUtils.isEmpty(name)) throw new AclNotValidException(Type.ACL_KEY_NOT_VALID, "An element of the "+ allow.getKey() + " ACL field has no name");
				boolean isRole =  isARole((ObjectNode)elemToCheck);
				if (!isRole){
					if (!UserService.exists(name))	throw new AclNotValidException(Type.ACL_USER_DOES_NOT_EXIST,"The user " + name + " does not exist");
				}else if (!RoleService.exists(name)) throw new AclNotValidException(Type.ACL_ROLE_DOES_NOT_EXIST,"The role " + name + " does not exist");
			}
		}
	}//check
	
	private  boolean isARole(ObjectNode elem) {
		boolean isRole =  ((ObjectNode)elem).get("isrole")==null?false: ((ObjectNode)elem).get("isrole").asBoolean();
		if (!isRole) isRole =  ((ObjectNode)elem).get("isRole")==null?false: ((ObjectNode)elem).get("isRole").asBoolean();
		return isRole;
	}

	public void empty() {
		if (this.aclJson==null) aclJson=new ObjectNode(JsonNodeFactory.instance);
		aclJson.set(BaasBoxPrivateFields.ALLOW_READ.toString(), new ArrayNode(JsonNodeFactory.instance));
		aclJson.set(BaasBoxPrivateFields.ALLOW_UPDATE.toString(), new ArrayNode(JsonNodeFactory.instance));
		aclJson.set(BaasBoxPrivateFields.ALLOW_DELETE.toString(), new ArrayNode(JsonNodeFactory.instance));
	}

}
