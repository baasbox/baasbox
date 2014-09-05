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

package com.baasbox.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import play.Logger;

import com.baasbox.configuration.Internal;
import com.baasbox.dao.IndexDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.RoleService;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabase.ATTRIBUTES;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Evolves the DB to the 0.7.0 schema
 * Main differences are on the Role management.
 * The roles now are:
 * 	-administrator
 * 	-registered
 * 	-backoffice
 * 	-anonymous
 * 
 * Each baasbox role inherits db permission from one of the embedded OrientDB default role:
 * 	-administrator --> admin + record blocking pass through
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
public class Evolution_0_7_0 implements IEvolution {
	private String version="0.7.0";
	
	public Evolution_0_7_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			edgeAndClassOptimization(db);	
			recreateDefaultRoles();
			db.getMetadata().getIndexManager().getIndex("ORole.name").rebuild();
			updateOldRoles();
			db.getMetadata().getIndexManager().getIndex("ORole.name").rebuild();		
			createNewIndexClass(db);
			updateIndices(db);
			updateDBVersion();
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void edgeAndClassOptimization(ODatabaseRecordTx db) {
		Logger.info("...enabling edges and vertexes optimization attributes..:");
		ATTRIBUTES attribute = ODatabase.ATTRIBUTES.CUSTOM;
		((ODatabaseComplex<?>) db).setInternal(attribute,  "useLightweightEdges=false");
		((ODatabaseComplex<?>) db).setInternal(attribute,  "useClassForEdgeLabel=false");
		((ODatabaseComplex<?>) db).setInternal(attribute,  "useClassForVertexLabel=false");
		((ODatabaseComplex<?>) db).setInternal(attribute,  "useVertexFieldsForEdgeLabels=false");
		Logger.info("...done...");
	}

	private void updateDBVersion(){
		Logger.info("changing db level version to " + version);
		Internal.DB_VERSION._setValue(version);
	}
	
	
	private void createNewIndexClass(ODatabaseRecordTx db){
		Logger.info("...creating INDEX CLASS...");
		OClass indexClass = db.getMetadata().getSchema().createClass(IndexDao.MODEL_NAME);
		OProperty keyProp = indexClass.createProperty("key", OType.STRING);
		keyProp.createIndex(INDEX_TYPE.UNIQUE);
		keyProp.setNotNull(true).setMandatory(true);
	}
	
	private void updateIndices(ODatabaseRecordTx db){
		List<String> indicesName = Arrays.asList(new String [] {
				"_bb_internal",
				"_bb_application",
				"_bb_images",
				"_bb_push",
				"_bb_social_login",
				"_bb_password_recovery"}
		);
		Logger.info("...migrating indices...");
		
		Collection indices= db.getMetadata().getIndexManager().getIndexes();
		for (Object in:indices){
			OIndex i = (OIndex)in;
			if (indicesName.contains(i.getName())){
				//migrate the index
				Logger.info("....." + i.getName());
				OIndexCursor cursor = i.cursor();
				Set<Entry<Object, OIdentifiable>> entries = cursor.toEntries();
				Iterator<Entry<Object, OIdentifiable>> it = entries.iterator();
				while (it.hasNext()){
					Entry<Object, OIdentifiable> entry = it.next();
					String key = (String) entry.getKey();
					Object valueOnDb=entry.getValue();
					valueOnDb=db.load((ORID)valueOnDb);
					if (valueOnDb!=null){
						Logger.info(".....   key: " + key);
						Object value=((ODocument)valueOnDb).field("value");
						String indexKey = i.getName().toUpperCase()+":"+key;
						ODocument newValue = new ODocument(IndexDao.MODEL_NAME);
						newValue.field("key",indexKey);
						newValue.field("value",value);
						newValue.save();
					}//the value is not null
				} //while
				db.getMetadata().getIndexManager().dropIndex(i.getName());
			}//the index is a baasbox index
			
		}//for each index defined on the db	
		
		Logger.info("...end indices migration");
	}//update indices
	
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
		backRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
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
		oldAdminRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
		oldAdminRole.getDocument().field("name",DefaultRoles.ADMIN.toString()+"1");

		//the new one must become the old one
		adminRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
		adminRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
		adminRole.getDocument().field(RoleService.FIELD_DESCRIPTION,DefaultRoles.BASE_ADMIN.getDescription());	
		adminRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,DefaultRoles.BASE_ADMIN.isAssignable());
		adminRole.getDocument().field(RoleDao.FIELD_INHERITED,(ORecord)null);
		oldAdminRole.addRule(ODatabaseSecurityResources.BYPASS_RESTRICTED, ORole.PERMISSION_ALL);
		adminRole.getDocument().field("name",DefaultRoles.BASE_ADMIN.toString());

		oldAdminRole.save();
		adminRole.save();
		oldAdminRole.getDocument().field(RoleDao.FIELD_INHERITED,adminRole.getDocument().getRecord());
		oldAdminRole.getDocument().field("name",DefaultRoles.ADMIN.toString());
		oldAdminRole.save();
		
		//update the "friend_of" roles
		
	}

}
