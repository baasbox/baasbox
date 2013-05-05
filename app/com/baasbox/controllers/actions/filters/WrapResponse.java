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
package com.baasbox.controllers.actions.filters;

import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;


public class WrapResponse extends Action.Simple {

	private ObjectNode prepareError(RequestHeader request, String error) {
		ObjectNode result = Json.newObject();
		  result.put("bb_code", "");
		  result.put("message", error);
		  result.put("resource", request.path());
		return result;
	} 
	
	private Result onUnauthorized(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 401);
		  return unauthorized(result);
	}  
	  
	private Result onForbidden(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 403);
		  return forbidden(result);
	}
	  
	private Result onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  return badRequest(result);
	} 
	
  
    public Result onResourceNotFound(RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 404);
		  return notFound(result);
    }
    
    public Result onDefaultError(int statusCode,RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", statusCode);
		  return Results.status(statusCode,result);
	}

	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Result result = delegate.call(ctx);
		
		final int statusCode = JavaResultExtractor.getStatus(result);
	    if (statusCode>399){	//an error has occured
		      final byte[] body = JavaResultExtractor.getBody(result);
		      String stringBody = new String(body, "UTF-8");
		      switch (statusCode) {
		      	case 400: return onBadRequest(ctx.request(),stringBody);
		      	case 401: return onUnauthorized(ctx.request(),stringBody);
		      	case 403: return onForbidden(ctx.request(),stringBody);
		      	case 404: return onResourceNotFound(ctx.request(),stringBody);
		      	default:  return onDefaultError(statusCode,ctx.request(),stringBody);
		      }
	    }
		Logger.trace("Method End");
	    return result;
	}

}
