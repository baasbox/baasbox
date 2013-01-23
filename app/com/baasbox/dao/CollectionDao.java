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

import java.util.List;

import play.Logger;



import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class CollectionDao extends NodeDao {
	private final static String MODEL_NAME="collection";
	public final static String NAME="name";
	
	public static CollectionDao getInstance(){
		return new CollectionDao();
	}
	
	protected CollectionDao() {
		super(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create(){
		throw new IllegalAccessError("To create a new document collection call create(String collectionName)");
	}
	
	/***
	 * Creates an entry into the ODocument-Collection and create a new Class named "collectionName"
	 * @param collectionName
	 * @return
	 * @throws Throwable 
	 */
	public ODocument create(String collectionName) throws Throwable {
		Logger.trace("Method Start");
		try {
			if (existsCollection(collectionName)) throw new InvalidCollectionException("Collection " + collectionName + " already exists");
		}catch (SqlInjectionException e){
			throw new InvalidCollectionException(e);
		}
		ODocument doc = super.create();
		doc.field("name",collectionName);
		save(doc);
		
		//create new class
		OClass documentClass = db.getMetadata().getSchema().getClass(CLASS_NODE_NAME);
		db.getMetadata().getSchema().createClass(collectionName, documentClass);
		
		//grants to the new class
		ORole registeredRole = RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString());
		ORole anonymousRole = RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString());
		registeredRole.addRule(ODatabaseSecurityResources.CLASS + "." + collectionName, ORole.PERMISSION_ALL);
		registeredRole.addRule(ODatabaseSecurityResources.CLUSTER + "." + collectionName, ORole.PERMISSION_ALL);
		anonymousRole.addRule(ODatabaseSecurityResources.CLASS + "." + collectionName, ORole.PERMISSION_READ);
		anonymousRole.addRule(ODatabaseSecurityResources.CLUSTER + "." + collectionName, ORole.PERMISSION_READ);
		PermissionsHelper.grantRead(doc, registeredRole);
		PermissionsHelper.grantRead(doc, anonymousRole);
		Logger.trace("Method End");
		return doc;
	}//getNewModelInstance(String collectionName)
	
	public boolean existsCollection(String collectionName) throws SqlInjectionException{
		Logger.trace("Method Start");
		QueryParams criteria = QueryParams.getInstance().where(NAME + "=?").params(new String [] {collectionName});
		List<ODocument> resultList= super.get(criteria);
		Logger.trace("Method End");
		return (resultList.size()>0) ;
	}
	
	public ODocument getByName(String collectionName) throws SqlInjectionException{
		Logger.trace("Method Start");
		ODocument result=null;
		QueryParams criteria = QueryParams.getInstance().where(NAME + "=?").params(new String [] {collectionName});
		List<ODocument> resultList= super.get(criteria);
		if (resultList!=null && resultList.size()>0) result=resultList.get(0);
		Logger.trace("Method End");
		return result;
	}
}
