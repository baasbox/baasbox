package com.baasbox.dao;

import java.util.List;
import java.util.UUID;

import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.storage.StorageUtils;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class LinkDao {
	
	private static final String QUERY_BASE="select *,out._node as out,in._node as in from E ";
	private static final String VIEW_BASE=" (select *,out._node as out,in._node as in from E) ";
	

	public static LinkDao getInstance(){
		return new LinkDao();
	}
	
	private LinkDao(){}
	
	public ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		OrientVertex sourceVertex = StorageUtils.getNodeVertex(sourceId);
		OrientVertex destVertex = StorageUtils.getNodeVertex(destId);
		UUID token = UUID.randomUUID();
		OrientEdge edge = (OrientEdge)sourceVertex.addEdge(edgeName, destVertex);
		edge.getRecord().field(BaasBoxPrivateFields.ID.toString(),token.toString());
		edge.getRecord().field(BaasBoxPrivateFields.AUTHOR.toString(),DbHelper.currentUsername());
		edge.save();
		//TODO: when we will support transactions, this will have to be changed 
		edge.getGraph().commit();
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
}
