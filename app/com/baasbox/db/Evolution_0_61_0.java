package com.baasbox.db;

import play.Logger;

import com.baasbox.configuration.Internal;
import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.role.RoleService;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecord;

/**
 * Evolves the DB to the 0.61.0 schema
 * Main differences are on the Role management.
 * The roles now are:
 * 	-administrator
 * 	-registered
 * 	-backoffice
 * 	-anonymous
 * 
 * Each baasbox role inherits db permission from one of the embedded OrientDB default role:
 * 	-administrator --> admin
 * 	-anonymous	   --> reader
 * 	-registered	   --> writer
 *  -backoffice    --> writer + record blocking pass through
 *  
 *  Furthermore each role has a set of parameters and flag to help the management of new custom roles:
 *  "internal": indicates if the role is created by baasbox (or is an orientDB default one), or if it is a custom role
 *  "modifiable": indicates if the role could be changed by the baasbox admin
 *  "assignable": inidicates if the role can be assigned to users
 *  "description": contains a short description of the role
 *  
 * @author Claudio Tesoriero
 *
 */
public class Evolution_0_61_0 implements IEvolution {
	private String version="0.61.0";
	
	public Evolution_0_61_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(OGraphDatabase db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
		recreateDefaultRoles();
		db.getMetadata().getIndexManager().getIndex("ORole.name").rebuild();
		updateOldRoles();
		db.getMetadata().getIndexManager().getIndex("ORole.name").rebuild();
		updateDBVersion();
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void updateDBVersion(){
		Logger.info("changing db level version to " + version);
		Internal.DB_VERSION.setValue(version);
	}
	
	private void recreateDefaultRoles(){
		Logger.info("Ricreating default roles");
		Logger.info("reader");
		ORole anonymRole = RoleDao.getRole("anonymoususer");
		ORole reader = RoleDao.createRole(DefaultRoles.BASE_READER.toString(), anonymRole.getMode(),anonymRole.getRules());
		
		reader.getDocument().field(RoleService.FIELD_INTERNAL,true);
		reader.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		reader.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.BASE_READER.getDescription());	
		reader.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.BASE_READER.isAssignable());
		reader.save();
		
		Logger.info("writer");;
		ORole regRole = RoleDao.getRole("registereduser");
		ORole writer = RoleDao.createRole(DefaultRoles.BASE_WRITER.toString(), regRole.getMode(),regRole.getRules());
		writer.getDocument().field(RoleService.FIELD_INTERNAL,true);
		writer.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		writer.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.BASE_WRITER.getDescription());	
		writer.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.BASE_WRITER.isAssignable());
		writer.save();
	}
	
	private void updateOldRoles(){
		Logger.info("Updating old roles");
		Logger.info("anonymoususer");
		ORole anonymRole = RoleDao.getRole("anonymoususer");
		anonymRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		anonymRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		anonymRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.ANONYMOUS_USER.getDescription());	
		anonymRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.ANONYMOUS_USER.isAssignable());
		anonymRole.getDocument().field(RoleDao.FIELD_INHERITED,RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.getInheritsFrom()).getDocument().getRecord());
		anonymRole.getDocument().field("name",DefaultRoles.ANONYMOUS_USER.toString());
		anonymRole.save();
		anonymRole=null;
		
		Logger.info("registered");
		ORole regRole = RoleDao.getRole("registereduser");
		regRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		regRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		regRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.REGISTERED_USER.getDescription());	
		regRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.REGISTERED_USER.isAssignable());
		regRole.getDocument().field(RoleDao.FIELD_INHERITED,RoleDao.getRole(DefaultRoles.REGISTERED_USER.getInheritsFrom()).getDocument().getRecord());
		regRole.getDocument().field("name",DefaultRoles.REGISTERED_USER.toString());
		regRole.save();
		regRole=null;
		
		Logger.info("backofficeuser");
		ORole backRole = RoleDao.getRole("backofficeuser");
		backRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		backRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		backRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.BACKOFFICE_USER.getDescription());	
		backRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.BACKOFFICE_USER.isAssignable());
		backRole.getDocument().field(RoleDao.FIELD_INHERITED,RoleDao.getRole(DefaultRoles.BACKOFFICE_USER.getInheritsFrom()).getDocument().getRecord());
		backRole.getDocument().field("name",DefaultRoles.BACKOFFICE_USER.toString());
		backRole.save();
		backRole=null;
		
		Logger.info("administrator");
		//retrieves the "old" admin role
		ORole oldAdminRole = RoleDao.getRole("admin");
		//duplicates it
		ORole adminRole = RoleDao.createRole(DefaultRoles.BASE_ADMIN+"_".toString(), oldAdminRole.getMode(),oldAdminRole.getRules());

		
		//now the old one must become the new one
		oldAdminRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		oldAdminRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		oldAdminRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.ADMIN.getDescription());	
		oldAdminRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.ADMIN.isAssignable());
		oldAdminRole.getDocument().field("name",DefaultRoles.ADMIN.toString()+"1");

		//the new one must become the old one
		adminRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		adminRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		adminRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.BASE_ADMIN.getDescription());	
		adminRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.BASE_ADMIN.isAssignable());
		adminRole.getDocument().field(RoleDao.FIELD_INHERITED,(ORecord)null);
		adminRole.getDocument().field("name",DefaultRoles.BASE_ADMIN.toString());

		oldAdminRole.save();
		adminRole.save();
		oldAdminRole.getDocument().field(RoleDao.FIELD_INHERITED,adminRole.getDocument().getRecord());
		oldAdminRole.getDocument().field("name",DefaultRoles.ADMIN.toString());
		oldAdminRole.save();
	}

}
