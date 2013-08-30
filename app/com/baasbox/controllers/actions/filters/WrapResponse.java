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

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import play.Logger;
import play.api.mvc.ChunkedResult;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import com.baasbox.BBConfiguration;


public class WrapResponse {

	
	private ObjectNode prepareError(RequestHeader request, String error) {
		org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
		ObjectNode result = Json.newObject();
		result.put("result", "error");
		result.put("bb_code", "");
		result.put("message", error);
		result.put("resource", request.path());
		result.put("method", request.method());
		result.put("request_header", mapper.valueToTree(request.headers()));
		result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
		return result;
	} 
	
	private Result onUnauthorized(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 401);
		   return Results.unauthorized(result);
		  
	}  
	  
	private Result onForbidden(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 403);
		  return Results.forbidden(result);
	}
	  
	private Result onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  return  Results.badRequest(result);
		  
	} 
	
  
    private Result onResourceNotFound(RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 404);
		  return Results.notFound(result);
		  
    }
    
    private Result onDefaultError(int statusCode,RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", statusCode);
		  return  Results.status(statusCode,result);
	}

	private Result onOk(int statusCode,Request request, String stringBody) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode result = Json.newObject();
		result.put("result", "ok");
		result.put("http_code", statusCode);
		try {
			result.put("data", mapper.readTree(stringBody));
		} catch (JsonProcessingException e) {
			result.put("data", stringBody);
		} catch (IOException e) {
			if (stringBody.isEmpty()) result.put("data", "");
			else throw new IOException("Error parsing stringBody: " + stringBody,e);
		}
		return Results.status(statusCode,result); 
	}

	public Result wrap(Context ctx, Result result) throws Throwable {
		Logger.trace("Method Start");
		
		ctx.response().setHeader("Access-Control-Allow-Origin", "*");
		//this is an hack because scala can't access to the http context, and we need this information for the access log
		String username=(String) ctx.args.get("username");
		if (username!=null) ctx.response().setHeader("BB-USERNAME", username);
		
		if (BBConfiguration.getWrapResponse()){
			Logger.debug("Wrapping the response");
			final int statusCode = JavaResultExtractor.getStatus(result);
			Logger.debug("Executed API: "  + ctx.request() + " , return code " + statusCode);
			Logger.debug("Result type:"+result.getWrappedResult().getClass().getName() + " Content-Type:" +ctx.response().getHeaders().get("Content-Type"));
			if (ctx.response().getHeaders().get("Content-Type")!=null 
		    		&& 
		    	!ctx.response().getHeaders().get("Content-Type").contains("json")){
		    	Logger.debug("The response is a file, no wrap will be applied");
		    	return result;
		    }
		    
		    if(result.getWrappedResult() instanceof ChunkedResult<?>){
		    	return result;
		    }
		    	
			final byte[] body = JavaResultExtractor.getBody(result);
		    String stringBody = new String(body, "UTF-8");
		    Logger.trace ("stringBody: " +stringBody);
			if (statusCode>399){	//an error has occured
			      switch (statusCode) {
			      	case 400: 	result =onBadRequest(ctx.request(),stringBody);
			      				break;
			      	case 401: 	result =onUnauthorized(ctx.request(),stringBody);
			      				break;
			      	case 403: 	result =onForbidden(ctx.request(),stringBody);
			      				break;
			      	case 404: 	result =onResourceNotFound(ctx.request(),stringBody);
			      				break;
			      	default:  	result =onDefaultError(statusCode,ctx.request(),stringBody);
			      				break;
			      }
		    }else{ //status is not an error
		    	result=onOk(statusCode,ctx.request(),stringBody);
		    } //if (statusCode>399)
			//We was expecting that this would be done by the framework, apparently this is false 
			ctx.response().setHeader("Content-Length",String.valueOf(JavaResultExtractor.getBody(result).length));
		}else{ //if (BBConfiguration.getWrapResponse())
			Logger.debug("The response will not be wrapped due configuration parameter");
		}

	    Logger.debug("  + result: \n" + result.toString());
		Logger.trace("Method End");
	    return result;
	}//wrap



}
