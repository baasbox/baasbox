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

import play.Logger;

import com.baasbox.dao.exception.CollectionAlreadyExistsException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.OpenTransactionException;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class CollectionDao extends NodeDao {
	private final static String MODEL_NAME="_BB_Collection";
	public final static String NAME="name";
	private static final String COLLECTION_NAME_INDEX = "_BB_Collection.name";
	
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
	 * @return an ODocument instance representing the collection
	 * @throws CollectionAlreadyExistsException 
	 * @throws OpenTransactionException, CollectionAlreadyExistsException, InvalidCollectionException, InvalidModelException, Throwable
	 */
	public ODocument create(String collectionName) throws OpenTransactionException, CollectionAlreadyExistsException, InvalidCollectionException, InvalidModelException, Throwable {
		if (DbHelper.isInTransaction()) throw new OpenTransactionException("Cannot create a collection within an open transaction");
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		try {
			if (existsCollection(collectionName)) throw new CollectionAlreadyExistsException("Collection " + collectionName + " already exists");
		}catch (SqlInjectionException e){
			throw new InvalidCollectionException(e);
		}
		if (Character.isDigit(collectionName.charAt(0))){
			throw new InvalidCollectionException("Collection names cannot start by a digit");
		}
		ODocument doc = super.create();
		doc.field("name",collectionName);
		if(collectionName.toUpperCase().startsWith("_BB_")){
			throw new InvalidCollectionException("Collection name is not valid: it can't be prefixed with _BB_");
		}
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
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return doc;
	}//getNewModelInstance(String collectionName)
	
	public boolean existsCollection(String collectionName) throws SqlInjectionException{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		OIndex idx = db.getMetadata().getIndexManager().getIndex(COLLECTION_NAME_INDEX);
		OIdentifiable record = (OIdentifiable) idx.get( collectionName );
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return (record!=null) ;
	}
	
	public ODocument getByName(String collectionName) throws SqlInjectionException{
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		OIndex idx = db.getMetadata().getIndexManager().getIndex(COLLECTION_NAME_INDEX);
		OIdentifiable record = (OIdentifiable) idx.get( collectionName );
		if (record==null) return null;
		return db.load(record.getIdentity());
	}
	
	@Override
	public void delete(String name) throws Exception{
		if (!existsCollection(name)) throw new InvalidCollectionException("Collection " + name + " does not exists");
		
		//get the helper class
		GenericDao gdao = GenericDao.getInstance();
		
		//begin transaction
		DbHelper.requestTransaction();
		
		try {
			//delete all vertices linked to objects in this class
			String deleteVertices = 
					"delete vertex _bb_nodevertex where _node.@class=?";
			Object[] params={name};
			gdao.executeCommand(deleteVertices, params);
			
			//delete vertices linked to the collection entry in the _bb_collection class
			//note: the params are equals to the previous one (just the collection name)
			String deleteVertices2="delete vertex _bb_nodevertex where _node.@class='_bb_collection' and _node.name=?";
			gdao.executeCommand(deleteVertices2, params);
			
			
			//delete this collection from the list of declared collections
			//note: the params are equals to the previous one (just the collection name)
			String deleteFromCollections= "delete from _bb_collection where name =?";
			gdao.executeCommand(deleteFromCollections, params);
			
			//delete all records belonging to the dropping collection....
			//it could be done dropping the class, but in this case we not should be able to perform a rollback
			String deleteAllRecords= "delete from " + name;
			gdao.executeCommand(deleteAllRecords, new Object[] {});
			
			//commit
			DbHelper.commitTransaction();
			
			//drop the collection class outside collection
			String dropCollection= "drop class " + name;
			gdao.executeCommand(dropCollection, new Object[] {});
			
		} catch (Exception e) {
			//rollback in case of error
			DbHelper.rollbackTransaction();
			if (Logger.isDebugEnabled()) Logger.debug ("An error occured deleting the collection " + name, e);
			throw e;
		}
	}//delete
}
