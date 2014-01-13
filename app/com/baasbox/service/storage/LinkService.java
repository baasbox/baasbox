package com.baasbox.service.storage;

import com.baasbox.dao.LinkDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class LinkService {

	public static ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		return LinkDao.getInstance().createLink(sourceId, destId, edgeName);
	}
	
	public static ODocument getLink(String linkId) {
		return LinkDao.getInstance().getLink(linkId); 
	}

}
