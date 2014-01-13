package com.baasbox.controllers;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.service.storage.LinkService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.JSONFormats.Formats;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Link extends Controller{

	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result createLink(String sourceId, String destId, String edgeName){
		ODocument toReturn = null;
		try {
			ODocument link = LinkService.createLink(sourceId, destId, edgeName);
			toReturn = (ODocument)link.getRecord().copy();
			//toReturn.detach();
			toReturn.field("out",((ODocument)(link.field("out"))).field("_node"));
			toReturn.field("in",((ODocument)(link.field("in"))).field("_node"));
		}catch (DocumentNotFoundException e){
			return badRequest("Source or Destination record was not found. Hint: do you have the read grant on them? Or ids are not valid");
		}
		toReturn.detach();
		return ok(JSONFormats.prepareResponseToJson(toReturn, Formats.LINK));
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result getLinkByItsId(String linkId){
		ODocument link=LinkService.getLink(linkId);
		if (link==null) return notFound("The link " + linkId + " was not found");
		return ok(JSONFormats.prepareResponseToJson(link, Formats.LINK));
	}
	
	/*
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getLinksFromNode(String nodeId){
		ODocument link=LinkService.getLink(linkId);
		if (link==null) notFound("The link " + linkId + " was not found");
		return ok(JSONFormats.prepareResponseToJson(link, Formats.LINK));
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class,ExtractQueryParameters.class})
	public static Result getLinksFromNode(String nodeId, String linkName){
		ODocument link=LinkService.getLink(linkId);
		if (link==null) notFound("The link " + linkId + " was not found");
		return ok(JSONFormats.prepareResponseToJson(link, Formats.LINK));
	}
	*/

}
