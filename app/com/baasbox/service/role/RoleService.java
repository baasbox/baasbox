package com.baasbox.service.role;

import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.RoleAlreadyExistsException;
import com.baasbox.exception.RoleNotFoundException;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;

public class RoleService {
	public static final String FIELD_INTERNAL="internal";
	public static final String FIELD_MODIFIABLE="modifiable";
	public static final String FIELD_ASSIGNABLE="assignable";
	public static final String FIELD_DESCRIPTION="description";
	
	
	
	/***
	 * Creates a new role inheriting permissions from another one
	 * @param name
	 * @param inheritedRole
	 * @param description
	 * @throws RoleNotFoundException if inehritedRole does not exist
	 * @throws RoleAlreadyExistsException if the 'name' role already exists
	 */
	public static void createRole(String name, String inheritedRole, String description) throws RoleNotFoundException, RoleAlreadyExistsException{
		if (!RoleDao.exists(inheritedRole)) throw new RoleNotFoundException(inheritedRole + " role does not exist!");
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
			if (!r.isOrientRole()){ //creates the new baasbox role
				newRole = RoleDao.createRole(r.toString(), r.getInheritsFrom());
			}else{	//retrieve the existing OrientDB role
				newRole=r.getORole();
			}
			newRole.getDocument().field(FIELD_INTERNAL,true);
			newRole.getDocument().field(FIELD_MODIFIABLE,false);
			newRole.getDocument().field(FIELD_DESCRIPTION,r.getDescription());	
			newRole.getDocument().field(FIELD_ASSIGNABLE,r.isAssignable());
			if (r==DefaultRoles.BACKOFFICE_USER) newRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_READ);
			newRole.save();
		}

	}
	
	
}
