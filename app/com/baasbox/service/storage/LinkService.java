package com.baasbox.service.storage;

import java.util.List;

import com.baasbox.dao.LinkDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class LinkService {

	public static ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		return LinkDao.getInstance().createLink(sourceId, destId, edgeName);
	}
	
	public static ODocument getLink(String linkId) {
		return LinkDao.getInstance().getLink(linkId); 
	}
	
	public static List<ODocument> getLink(QueryParams criteria) throws SqlInjectionException {
		return LinkDao.getInstance().getLinks(criteria);
	}

	public static void deleteLink(String linkId) {
		LinkDao.getInstance().deleteLink(linkId); 
	}

}
