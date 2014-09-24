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

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.Logger;
import play.api.mvc.ChunkedResult;
import play.core.j.JavaResultExtractor;
import play.libs.Json;
import play.mvc.Http.Context;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.CustomHttpCode;

import play.mvc.SimpleResult;
import play.libs.F;

public class WrapResponse {

	
	private ObjectNode prepareError(RequestHeader request, String error) {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		ObjectNode result = Json.newObject();
		result.put("result", "error");
		result.put("message", error);
		result.put("resource", request.path());
		result.put("method", request.method());
		result.put("request_header", (JsonNode)mapper.valueToTree(request.headers()));
		result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
		setCallIdOnResult(request, result);
		return result;
	} 

	
	private SimpleResult onCustomCode(int statusCode, RequestHeader request, String data) throws IOException {
		CustomHttpCode customCode = CustomHttpCode.getFromBbCode(statusCode);
		ObjectNode result=null;
		if (customCode.getType().equals("error")){
			result = prepareError(request, data);
		}else{
			result= prepareOK(statusCode, request, data);
		}
		result.put("http_code", customCode.getHttpCode());
		result.put("bb_code", String.valueOf(customCode.getBbCode()));
		return Results.status(customCode.getHttpCode(), result);	
	}
	
	
	private SimpleResult onUnauthorized(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 401);
		   return Results.unauthorized(result);
		  
	}  
	  
	private SimpleResult onForbidden(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 403);
		  return Results.forbidden(result);
	}
	  
	private SimpleResult onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  return  Results.badRequest(result);
		  
	} 
	
  
    private SimpleResult onResourceNotFound(RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 404);
		  return Results.notFound(result);
		  
    }
    
    private SimpleResult onDefaultError(int statusCode,RequestHeader request,String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", statusCode);
		  return  Results.status(statusCode,result);
	}

    private ObjectNode prepareOK(int statusCode,RequestHeader request, String stringBody) throws IOException{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode result = Json.newObject();
		setCallIdOnResult(request, result);
		result.put("result", "ok");
		try {
			result.put("data", mapper.readTree(stringBody));
		} catch (JsonProcessingException e) {
			result.put("data", stringBody);
		} catch (IOException e) {
			if (stringBody.isEmpty()) result.put("data", "");
			else throw new IOException("Error parsing stringBody: " + stringBody,e);
		}    
		return result;
    }


	/**
	 * @param request
	 * @param result
	 */
	private void setCallIdOnResult(RequestHeader request, ObjectNode result) {
		String callId = request.getQueryString("call_id");
		if (!StringUtils.isEmpty(callId)) result.put("call_id",callId);
	}
    
	private SimpleResult onOk(int statusCode,RequestHeader request, String stringBody) throws IOException  {
		ObjectNode result = prepareOK(statusCode, request, stringBody);
		result.put("http_code", statusCode);
		return Results.status(statusCode,result); 
	}

	public SimpleResult wrap(Context ctx, F.Promise<SimpleResult> simpleResult) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		
		SimpleResult result=simpleResult.get(10000);
		ctx.response().setHeader("Access-Control-Allow-Origin", "*");
		ctx.response().setHeader("Access-Control-Allow-Headers", "X-Requested-With");
		//this is an hack because scala can't access to the http context, and we need this information for the access log
		String username=(String) ctx.args.get("username");
		if (username!=null) ctx.response().setHeader("BB-USERNAME", username);
		
	    byte[] resultContent=null;
		if (BBConfiguration.getWrapResponse()){
			if (Logger.isDebugEnabled()) Logger.debug("Wrapping the response");
			final int statusCode = result.getWrappedSimpleResult().header().status();
			if (Logger.isDebugEnabled()) Logger.debug("Executed API: "  + ctx.request() + " , return code " + statusCode);
			if (Logger.isDebugEnabled()) Logger.debug("Result type:"+result.getWrappedResult().getClass().getName() + " Response Content-Type:" +ctx.response().getHeaders().get("Content-Type"));
			if (ctx.response().getHeaders().get("Content-Type")!=null 
		    		&& 
		    	!ctx.response().getHeaders().get("Content-Type").contains("json")){
		    	if (Logger.isDebugEnabled()) Logger.debug("The response is a file, no wrap will be applied");
		    	return result;
		    }
		    
		    if(result.getWrappedResult() instanceof ChunkedResult<?>){
		    	return result;
		    }
		    	
			final byte[] body = JavaResultExtractor.getBody(result);
			String stringBody = new String(body, "UTF-8");
		    if (Logger.isTraceEnabled()) if (Logger.isTraceEnabled()) Logger.trace ("stringBody: " +stringBody);
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
			      	default:  	
			      		if (CustomHttpCode.getFromBbCode(statusCode)!=null){
			      	        result = onCustomCode(statusCode,ctx.request(),stringBody);		
			      		}else result =onDefaultError(statusCode,ctx.request(),stringBody);
			      	break;
			      }
		    }else{ //status is not an error
		    	result=onOk(statusCode,ctx.request(),stringBody);
		    } //if (statusCode>399)
			if (statusCode==204) result = Results.noContent();
			try {
				if (Logger.isDebugEnabled()) Logger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
			}catch (Throwable e){}
		}else{ //if (BBConfiguration.getWrapResponse())
			if (Logger.isDebugEnabled()) Logger.debug("The response will not be wrapped due configuration parameter");
			try {
				if (Logger.isDebugEnabled()) Logger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
			}catch (Throwable e){}
			if (Logger.isDebugEnabled()) Logger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
		}
		ctx.response().setHeader("Content-Length", Long.toString(JavaResultExtractor.getBody(result).length));
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return result;
	}//wrap





}
