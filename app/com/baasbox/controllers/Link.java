package com.baasbox.controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.service.storage.LinkService;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Link extends Controller{

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result createLink(String sourceId, String destId, String edgeName){
		ODocument link = null;
		try {
			link = LinkService.createLink(sourceId, destId, edgeName);
		}catch (DocumentNotFoundException e){
			return notFound("Source or Destination record not found. Hint: do you have the read grant on them?");
		}
		return ok(link.toJSON());
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getLink(String sourceId, String destId, String edgeName){
		try {
			LinkService.createLink(sourceId, destId, edgeName);
		}catch (DocumentNotFoundException e){
			return notFound("Source or Destination record not found. Hint: do you have the read grant on them?");
		}
		return ok();
	}

}
