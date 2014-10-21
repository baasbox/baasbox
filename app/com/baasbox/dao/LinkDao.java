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

package com.baasbox.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import play.Logger;

import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.storage.StorageUtils;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class LinkDao {
	public static final String MODEL_NAME = "E";
	
	//private static final String QUERY_BASE="select *,out._node as from,in._node as to from E ";
	private static final String QUERY_BASE="select *,out._node as out,in._node as in from " + MODEL_NAME;
	private static final String VIEW_BASE=" (" + QUERY_BASE + ") ";
	

	public static LinkDao getInstance(){
		return new LinkDao();
	}
	
	private LinkDao(){}
	
	public ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		DbHelper.requestTransaction();
		OrientEdge edge = null;
		try{
			OrientVertex sourceVertex = StorageUtils.getNodeVertex(sourceId);
			OrientVertex destVertex = StorageUtils.getNodeVertex(destId);
			UUID token = UUID.randomUUID();
			edge = (OrientEdge)sourceVertex.addEdge(edgeName, destVertex);
			edge.getRecord().field(BaasBoxPrivateFields.ID.toString(),token.toString());
			edge.getRecord().field(BaasBoxPrivateFields.AUTHOR.toString(),DbHelper.currentUsername());
			edge.getRecord().field(BaasBoxPrivateFields.CREATION_DATE.toString(),new Date());
			edge.save();
			DbHelper.commitTransaction();
		}catch (DocumentNotFoundException e){
			DbHelper.rollbackTransaction();
			throw e;
		}
		//edge.getGraph().commit();
		return edge.getRecord();
	}
	
	public ODocument getLink(String id){
		String getLinkById= QUERY_BASE + " where id = ?";
		List<ODocument> result=(List<ODocument>) DbHelper.genericSQLStatementExecute(getLinkById, new String[]{id});
		if (result!=null && !result.isEmpty()) return result.get(0);
		return null;
	}
	
	public List<ODocument> getLinks(QueryParams criteria) throws SqlInjectionException{
		GenericDao gdao = GenericDao.getInstance();
		return gdao.executeQuery(VIEW_BASE, criteria);
	}

	public void deleteLink(String linkId) {
		ORID linkRid = getRidLinkByUUID(linkId);
		DbHelper.getConnection().delete(linkRid);
	}
	
	/***
	 * Returns an edge (link), belonging to the class @LinkDao.MODEL_NAME, by its id (not RID)
	 * @param id
	 * @return
	 */
	public static ORID getRidLinkByUUID(String id){
		ODatabaseRecordTx db =DbHelper.getConnection();
		OIndex<?> index = db.getMetadata().getIndexManager().getIndex(LinkDao.MODEL_NAME + ".id");
		ORID rid = (ORID) index.get(id);  
		return rid;
	}
}
