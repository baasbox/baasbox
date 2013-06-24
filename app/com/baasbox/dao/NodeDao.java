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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import play.Logger;

import com.baasbox.BBInternalConstants;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DocumentNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OSQLHelper;


public abstract class NodeDao  {

	public final String MODEL_NAME;
	public static final String CLASS_NODE_NAME = "Node";
	public static final String CLASS_VERTEX_NAME = "NodeVertex";
	
	public static final String FIELD_LINK_TO_VERTEX = "_links";
	public static final String FIELD_TO_DOCUMENT_FIELD = "_node";
	public static final String FIELD_CREATION_DATE = "_creation_date";
	
	public static final String EDGE_CLASS_CREATED = "Created";
	

	protected OGraphDatabase db;

	 
	public NodeDao(String modelName) {
		super();
		this.MODEL_NAME=modelName;
		this.db=DbHelper.getConnection();
	}

	
	protected static HashMap<String,Object> backupBaasBoxFields(ODocument document){
		HashMap<String,Object> map = new HashMap<String,Object>();
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			map.put(r.toString(), document.field(r.toString()));
		}
		return map;
	}
	
	protected static ODocument restoreBaasBoxFields(ODocument document, HashMap<String,Object> map){
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			document.fields(r.toString(),map.get(r.toString()));
		}
		return document;
	}
	

	public void checkModelDocument(ODocument doc) throws InvalidModelException {
		if (doc!=null && !doc.getClassName().equalsIgnoreCase(this.MODEL_NAME))
			throw new InvalidModelException();
	}


	public ODocument create() throws Throwable {
		Logger.trace("Method Start");
		try{
				ODocument doc = new ODocument(this.MODEL_NAME);
				ODocument vertex = db.createVertex(CLASS_VERTEX_NAME);
				doc.field(FIELD_LINK_TO_VERTEX,vertex);
				doc.field(FIELD_CREATION_DATE,new Date());
				vertex.field(FIELD_TO_DOCUMENT_FIELD,doc);
				UUID token = UUID.randomUUID();
				Logger.debug("CreateUUID.onRecordBeforeCreate: " + doc.getIdentity() + " -->> " + token.toString());
				doc.field(BaasBoxPrivateFields.ID.toString(),token.toString());
				//crates a link (an edge) between the current user and the document  
				UserDao udao = new UserDao();
				ODocument user=udao.getByUserName(DbHelper.getCurrentUserName());
				ODocument userVertex=user.field(FIELD_LINK_TO_VERTEX);
				db.createEdge(userVertex,vertex,EDGE_CLASS_CREATED);
			Logger.trace("Method End");
			return doc;
		}catch (Throwable e){
			throw e;
		}
	}
	


	protected  void save(ODocument document) throws InvalidModelException {
		Logger.trace("Method Start");
		checkModelDocument(document);
		document.save();
		Logger.trace("Method End");
	}
	

	public void update(ODocument originalDocument, ODocument documentToMerge)  {
		//backup the baasbox's fields 
		HashMap<String,Object> map = backupBaasBoxFields(originalDocument);
		//update the document
		originalDocument.merge(documentToMerge, false, false);
		//restore the baasbox's fields
		restoreBaasBoxFields(originalDocument, map);
		originalDocument.save();
	}
	


	public List<ODocument> get(QueryParams criteria) throws SqlInjectionException {
		Logger.trace("Method Start");
		OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, false, criteria);
		List<ODocument> result = DbHelper.commandExecute(command, criteria.getParams());
		Logger.trace("Method End");
		return result;
	}


	public ODocument get(ORID rid) throws InvalidModelException, DocumentNotFoundException {
		Logger.trace("Method Start");
		Object doc=db.load(rid);
		if (doc==null) throw new DocumentNotFoundException();
		if (!(doc instanceof ODocument)) throw new IllegalArgumentException(rid +" is a rid not referencing a valid Document");
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the rid may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the rid " + rid + " is not valid belong to the collection " + this.MODEL_NAME);
		}
		Logger.trace("Method End");
		return (ODocument)doc;
	}


	public ODocument get(String rid) throws InvalidModelException, ODatabaseException, DocumentNotFoundException {
		Logger.trace("Method Start");
		Object orid=OSQLHelper.parseValue(rid, null);
		if ((orid==null) || !(orid instanceof ORecordId) || (orid.toString().equals(OSQLHelper.VALUE_NOT_PARSED))) throw new IllegalArgumentException(rid +" is not a valid rid");
		Object odoc=get((ORecordId)orid);
		
		Logger.trace("Method End");
		return (ODocument)odoc;
	}
 

	public boolean exists(ODocument document) throws InvalidModelException, DocumentNotFoundException {
		return exists(document.getRecord().getIdentity());
	}


	public boolean exists(ORID rid) throws InvalidModelException, DocumentNotFoundException {
		ODocument doc = get(rid);
		return (doc!=null);
	}


	public boolean exists(String rid) throws InvalidModelException, ODatabaseException, DocumentNotFoundException {
		ODocument doc = get(rid);
		return (doc!=null);
	}

	public ODocument revokePermission(ODocument document, Permissions permission, OUser user) {
		return PermissionsHelper.revoke(document, permission, user); 
	}

	public ODocument revokePermission(ODocument document, Permissions permission, ORole role) {
		return PermissionsHelper.revoke(document, permission, role);
	}
	
	public ODocument grantPermission(ODocument document, Permissions permission, OUser user) {
		return PermissionsHelper.grant(document, permission, user);
		 
	}

	public ODocument grantPermission(ODocument document, Permissions permission, ORole role) {
		return PermissionsHelper.grant(document, permission, role);
	}
	
	public ODocument grantPermission(ODocument document, Permissions permission, ORole[] roles) {
		for (ORole role : roles){
			grantPermission(document, permission, role);
		}
		return document;
	}

	
	public long getCount(){
		return db.countClass(MODEL_NAME);
	}
	
	public long getCount(QueryParams criteria) throws SqlInjectionException{
		Logger.trace("Method Start");
		OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, true, criteria);
		List<ODocument> result = DbHelper.commandExecute(command, criteria.getParams());
		Logger.trace("Method End");
		return ((Long)result.get(0).field("count")).longValue();
	}
	
	
	public void delete(String rid) throws Throwable{
		Logger.trace("Method Start");
		ODocument doc = get(rid);
		delete(doc.getIdentity());
		Logger.trace("Method End");
	}
	
	public void delete(ORID rid) throws Throwable{
		Logger.trace("Method Start");
		//retrieve the vertex associated to this node
		try{
			DbHelper.requestTransaction();
			ODocument vertex = (ODocument)((ODocument)db.load(rid)).field(FIELD_LINK_TO_VERTEX);
			db.removeVertex(vertex);
			db.delete(rid);
			DbHelper.commitTransaction();
		}catch (Throwable e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		Logger.trace("Method End");
	}
	
	
}