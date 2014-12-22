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

package com.baasbox.controllers;

import java.io.IOException;
import java.util.List;

import com.baasbox.controllers.actions.filters.*;
import com.baasbox.db.DbHelper;
import com.baasbox.util.ErrorToResult;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import play.mvc.Http.Context;

import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.storage.LinkService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.JSONFormats.Formats;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Link extends Controller{

	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> createLink(String sourceId, String destId, String edgeName){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
				()->{
					ODocument link = LinkService.createLink(sourceId,destId,edgeName);
					ODocument toReturn =(ODocument)link.getRecord().copy();
					toReturn.field("out",(ODocument)((ODocument)(link.field("out"))).field("_node"));
					toReturn.field("in",(ODocument)((ODocument)(link.field("in"))).field("_node"));
					toReturn.detach();
					return ok(JSONFormats.prepareResponseToJson(toReturn,Formats.LINK));
				})).recover(ErrorToResult
				.when(DocumentNotFoundException.class,
						e ->  badRequest("Source or Destination record was not found. Hint: do you have the read grant on them? Or ids are not valid"))
				.when(Throwable.class,
						e -> internalServerError(e.getMessage()==null?"":e.getMessage())));
	}
	
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> getLinkByItsId(String linkId){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
				()->{
					ODocument link=LinkService.getLink(linkId);
					if (link==null) {
						return notFound("The link " + linkId + " was not found");
					} else {
						return ok(JSONFormats.prepareResponseToJson(link, Formats.LINK));
					}
				}));
	}
	
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class,ExtractQueryParameters.class})
	public static F.Promise<Result> getLinks() throws IOException{
		Context ctx=Http.Context.current.get();
		QueryParams criteria = (QueryParams) ctx.args.get(IQueryParametersKeys.QUERY_PARAMETERS);

		return F.Promise.promise(DbHelper.withDbFromContext(ctx,()->{
			List<ODocument> links = LinkService.getLink(criteria);
			return ok(JSONFormats.prepareResponseToJson(links,Formats.LINK));
		})).recover(ErrorToResult
			.when(InvalidCriteriaException.class,
					e -> badRequest(ExceptionUtils.getMessage(e)))
			.when(SqlInjectionException.class,
					e -> badRequest("The parameters you passed are incorrect. HINT: check if the querystring is correctly encoded")));
	}
	
	@With ({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> deleteLink(String linkId){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),
				()->{
					ODocument link = LinkService.getLink(linkId);
					if (link==null){
						return notFound("The link " + linkId + " was not found");
					} else {
						LinkService.deleteLink(linkId);
						return ok();
					}
				}));
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
