package com.baasbox.service.storage;

import java.util.UUID;

import com.baasbox.dao.exception.DocumentNotFoundException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class LinkService {

	public static ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		OrientVertex sourceVertex = StorageUtils.getNodeVertex(sourceId);
		OrientVertex destVertex = StorageUtils.getNodeVertex(destId);
		UUID token = UUID.randomUUID();
		OrientEdge edge = (OrientEdge)sourceVertex.addEdge(edgeName, destVertex);
		edge.getRecord().field(BaasBoxPrivateFields.ID.toString(),token.toString());
		edge.save();
		//TODO: when we will support transactions, this will have to be changed 
		edge.getGraph().commit();
		return edge.getRecord();
	}
	
	public static void getLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		OrientVertex sourceVertex = StorageUtils.getNodeVertex(sourceId);
		OrientVertex destVertex = StorageUtils.getNodeVertex(destId);
		
		OrientEdge edge = (OrientEdge)sourceVertex.addEdge(edgeName, destVertex);
		edge.save();
		//TODO: when we will support transactions, this will have to be changed 
		edge.getGraph().commit();
	}

}
