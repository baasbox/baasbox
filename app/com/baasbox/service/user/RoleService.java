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

package com.baasbox.service.user;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import play.Logger;

import com.baasbox.dao.GenericDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.RoleAlreadyExistsException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.RoleNotModifiableException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class RoleService {
	public static final String FIELD_INTERNAL="internal";
	public static final String FIELD_MODIFIABLE="modifiable";
	public static final String FIELD_ASSIGNABLE="assignable";
	public static final String FIELD_DESCRIPTION="description";
	
	
	public static boolean exists(String roleName){
		return RoleDao.exists(roleName);
	}
	
	/***
	 * Creates a new role inheriting permissions from another one
	 * @param name
	 * @param inheritedRole
	 * @param description
	 * @throws RoleNotFoundException if inehritedRole does not exist
	 * @throws RoleAlreadyExistsException if the 'name' role already exists
	 */
	public static void createRole(String name, String inheritedRole, String description) throws RoleNotFoundException, RoleAlreadyExistsException{
		if (!RoleDao.exists(inheritedRole)) {
			RoleNotFoundException e = new RoleNotFoundException(inheritedRole + " role does not exist!");
			e.setInehrited(true);
			throw e;
		}
		if (RoleDao.exists(name)) throw new RoleAlreadyExistsException(name + " role already exists!");
		ORole newRole = RoleDao.createRole(name, inheritedRole);
		newRole.getDocument().field(FIELD_INTERNAL,false);
		newRole.getDocument().field(FIELD_MODIFIABLE,true);
		newRole.getDocument().field(FIELD_DESCRIPTION,description);
		newRole.getDocument().field(FIELD_ASSIGNABLE,true);
		newRole.save();
	}
	
	public static void createInternalRoles(){
		for (DefaultRoles r : DefaultRoles.values()){
			ORole newRole;
			if (Logger.isDebugEnabled()) Logger.debug("creating " + r.toString() + "...");
			if (!r.isOrientRole()){ //creates the new baasbox role
				newRole = RoleDao.createRole(r.toString(), r.getInheritsFrom());
			}else{	//retrieve the existing OrientDB role
				newRole=r.getORole();
				newRole.reload();
			}
			newRole.getDocument().field(FIELD_INTERNAL,true);
			newRole.getDocument().field(FIELD_MODIFIABLE,false);
			newRole.getDocument().field(FIELD_DESCRIPTION,r.getDescription());	
			newRole.getDocument().field(FIELD_ASSIGNABLE,r.isAssignable());
			if (r==DefaultRoles.BACKOFFICE_USER) newRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
			if (r==DefaultRoles.ADMIN) newRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
			newRole.save();
		}

	}

	public static List<ODocument> getRoles() throws SqlInjectionException {
		GenericDao dao = GenericDao.getInstance();
		QueryParams criteria = QueryParams.getInstance().where("name not like \""+RoleDao.FRIENDS_OF_ROLE+"%\" and assignable=true").orderBy("name asc");
		return dao.executeQuery("orole", criteria);
	}
	
	/***
	 * Returns a list with a single record containing the ODocument (class ORole) referring to the role specified by the parameter
	 * @param name
	 * @return
	 * @throws SqlInjectionException
	 */
	public static List<ODocument> getRoles(String name) throws SqlInjectionException {
		GenericDao dao = GenericDao.getInstance();
		QueryParams criteria = QueryParams.getInstance().where("name = ? and assignable=true").params(new String[]{name}).orderBy("name asc");
		return dao.executeQuery("orole", criteria);
	}
	
	public static ORole getORole(String name)  {
		return RoleDao.getRole(name);
	}
	
	/**
	 * Edit a Role
	 * @param name	the role to edit
	 * @param newName	the new name to assign it
	 * @param inheritedRoleName the new inherited role to assign, if empty or null, it is ignored
	 * @param description new description. If null, it will be ignored
	 * @return
	 */
	public static void editRole(String name, String inheritedRole,
			String description, String newName) throws RoleNotFoundException, RoleNotModifiableException {

		if (!RoleDao.exists(name)) throw new RoleNotFoundException(name + " role does not exist!");
		ORole role = RoleDao.getRole(name);
		ODocument roleDoc=role.getDocument();
		if (roleDoc.field(FIELD_MODIFIABLE)==Boolean.FALSE) throw new RoleNotModifiableException(name + " role is not modifiable");
		if (!StringUtils.isEmpty(inheritedRole)) {
			if (!RoleDao.exists(inheritedRole)) {
				RoleNotFoundException e = new RoleNotFoundException(inheritedRole + " role does not exist!");
				e.setInehrited(true);
				throw e;
			}
			ORole roleIn=RoleDao.getRole(inheritedRole);
			roleDoc.field(RoleDao.FIELD_INHERITED,roleIn.getDocument().getRecord());
		}
		
		if (!StringUtils.isEmpty(newName)) roleDoc.field("name",newName);
		if (description!=null) roleDoc.field(FIELD_DESCRIPTION,description);
		roleDoc.save();
	}

	public static void delete(String name) throws RoleNotFoundException, RoleNotModifiableException {
		if (!RoleDao.exists(name)) throw new RoleNotFoundException(name + " role does not exist!");
		ORole role = RoleDao.getRole(name);
		if (role.getDocument().field(FIELD_INTERNAL)==Boolean.TRUE) throw new RoleNotModifiableException("Role " + name + " cannot be deleted. It is declared like 'internal'");
		//retrieve the users belonging to that role
		UserService.moveUsersToRole(name,DefaultRoles.REGISTERED_USER.toString());
		//delete the role
		RoleDao.delete(name);
	}

	public static boolean isAssignable(ORole newORole) {
		if (newORole==null) return false;
		if (newORole.getDocument().field(FIELD_ASSIGNABLE)==null || newORole.getDocument().field(FIELD_ASSIGNABLE)==Boolean.FALSE )
			return false;
		return true;
	}
	
	public static boolean hasRole(String username,String roleName){
		boolean result = false;
		if(UserDao.getInstance().existsUserName(username)){
			OUser user = UserService.getOUserByUsername(username);
			Set<ORole> roles = user.getRoles();
			if(roles!=null){
				for (ORole oRole : roles) {
					if(oRole.getName().equals(roleName)){
						result = true;
						break;
					}
				}
			}
		}
		return result;
	}
	
	/***
	 * Returns true if the user belongs to an administrative role
	 * @param username
	 * @return
	 */
	public static boolean roleCanByPassRestrictedAccess(String roleName){
		ORole role = getORole(roleName);
		return role.allow(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
	}
	

}
