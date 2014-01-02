package com.baasbox.service.storage;

import com.baasbox.dao.exception.DocumentNotFoundException;
import com.tinkerpop.blueprints.impls.orient.OrientEdge;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class LinkService {

	public static void createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		OrientVertex sourceVertex = StorageUtils.getNodeVertex(sourceId);
		OrientVertex destVertex = StorageUtils.getNodeVertex(destId);
		
		OrientEdge edge = (OrientEdge)sourceVertex.addEdge(edgeName, destVertex);
		edge.save();
		//TODO: when we will support transactions, this must be changed 
		edge.getGraph().commit();
	}

}
