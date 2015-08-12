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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import play.api.mvc.ChunkedResult;
import play.core.j.JavaResultExtractor;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Http.RequestHeader;
import play.mvc.Results;
import play.mvc.SimpleResult;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WrapResponse {
	/***
	 * Pattern should be thread safe: https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
	 * "Instances of this class are immutable and are safe for use by multiple concurrent threads. Instances of the Matcher class are not safe for such use."
	 * This pattern matches "at package.class.method(source_code:123)" 
	 * Note that "source_code" can be a java, scala or js file! 
	 */
	private static Pattern tracePattern = Pattern
            .compile("\\s*at\\s+([\\w\\.$_]+)\\.([\\w$_]+)(\\(.*\\..*)?:(\\d+)\\)(\\n|\\r\\n)");
	
	// This pattern matches " package.class.exception : optional message'" 
	private static Pattern headLinePattern = Pattern.compile("([\\w\\.]+)(:.*)?");
	
	/* inspired by https://stackoverflow.com/questions/10013713/reading-and-parsing-java-exceptions */ 
	private List<String> tryToExtractTheStackTrace(String error){
        Matcher traceMatcher = tracePattern.matcher(error);
        List<String> stackTrace = new ArrayList<String>();
        while (traceMatcher.find()) {
            String className = traceMatcher.group(1);
            String methodName = traceMatcher.group(2);
            String sourceFile = traceMatcher.group(3);
            int lineNum = Integer.parseInt(traceMatcher.group(4));
            stackTrace.add(new StringBuilder()
            					.append(className) 
            					.append(".") 
            					.append(methodName)
            					.append("(")
            					.append(sourceFile)
            					.append(":")
            					.append(lineNum)
            					.append(")")
            					.toString());
        }
        return stackTrace;
	}
	
	private ObjectNode prepareError(RequestHeader request, String error) {
		//List<StackTraceElement> st = this.tryToExtractTheStackTrace(error);
		List<String> st = this.tryToExtractTheStackTrace(error);
		
		com.fasterxml.jackson.databind.ObjectMapper mapper = BBJson.mapper();
		ObjectNode result = Json.newObject();
		result.put("result", "error");
		
		//the error is an exception or a plain message?
		if (st.size()==0){
			result.put("message", error);
		} else {
	        Matcher headLineMatcher = headLinePattern.matcher(error);
	        StringBuilder message = new StringBuilder();
	        if (headLineMatcher.find()) {
	        	message.append(headLineMatcher.group(1));
	        	if (headLineMatcher.group(2) != null) {
	                message.append(" ").append(headLineMatcher.group(2));
	            }
	        }
			result.put("message", message.toString());
			ArrayNode ston = result.putArray("stacktrace");
			st.forEach(x->{
				ston.add(x);
			});
			result.put("full_stacktrace",error);
		}
		result.put("resource", request.path());
		result.put("method", request.method());
		result.put("request_header", (JsonNode)mapper.valueToTree(request.headers()));
		result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
		result.put("db_schema_version", BBConfiguration.getDBVersion());
		this.setCallIdOnResult(request, result);
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
		ObjectMapper mapper = BBJson.mapper();
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

	private void setServerTime(Http.Response response) {
		ZonedDateTime date = ZonedDateTime.now(ZoneId.of("GMT"));
		String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(date);
		response.setHeader("Date",httpDate);
	}

	private SimpleResult onOk(int statusCode,RequestHeader request, String stringBody) throws IOException  {
		ObjectNode result = prepareOK(statusCode, request, stringBody);
		result.put("http_code", statusCode);
		return Results.status(statusCode,result); 
	}

	public SimpleResult wrap(Context ctx, F.Promise<SimpleResult> simpleResult) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		
		SimpleResult result=simpleResult.get(10000);
		ctx.response().setHeader("Access-Control-Allow-Origin", "*");
		ctx.response().setHeader("Access-Control-Allow-Headers", "X-Requested-With");
		//this is an hack because scala can't access to the http context, and we need this information for the access log
		String username=(String) ctx.args.get("username");
		if (username!=null) ctx.response().setHeader("BB-USERNAME", username);
		
	    byte[] resultContent=null;
		if (BBConfiguration.getWrapResponse()){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Wrapping the response");
			final int statusCode = result.getWrappedSimpleResult().header().status();
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Executed API: "  + ctx.request() + " , return code " + statusCode);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Result type:"+result.getWrappedResult().getClass().getName() + " Response Content-Type:" +ctx.response().getHeaders().get("Content-Type"));
			if (ctx.response().getHeaders().get("Content-Type")!=null 
		    		&& 
		    	!ctx.response().getHeaders().get("Content-Type").contains("json")){
		    	if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("The response is a file, no wrap will be applied");
		    	return result;
		    }
		    
		    if(result.getWrappedResult() instanceof ChunkedResult<?>){
		    	return result;
		    }
		    	
			final byte[] body = JavaResultExtractor.getBody(result);
			String stringBody = new String(body, "UTF-8");
		    if (BaasBoxLogger.isTraceEnabled()) if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace ("stringBody: " +stringBody);
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
			      		}else {
							result =onDefaultError(statusCode,ctx.request(),stringBody);
						}
			      	break;
			      }
		    }else{ //status is not an error
		    	result=onOk(statusCode,ctx.request(),stringBody);
		    } //if (statusCode>399)
			if (statusCode==204) result = Results.noContent();
			try {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
			}catch (Throwable e){}
		}else{ //if (BBConfiguration.getWrapResponse())
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("The response will not be wrapped due configuration parameter");
			try {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
			}catch (Throwable e){}
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
		}
		setServerTime(ctx.response());
		ctx.response().setHeader("Content-Length", Long.toString(JavaResultExtractor.getBody(result).length));
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return result;
	}//wrap


	public F.Promise<SimpleResult> wrapAsync(Context ctx, F.Promise<SimpleResult> simpleResult) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		
		return simpleResult.map((result)->{
			
			ctx.response().setHeader("Access-Control-Allow-Origin", "*");
			ctx.response().setHeader("Access-Control-Allow-Headers", "X-Requested-With");
			//this is an hack because scala can't access to the http context, and we need this information for the access log
			String username=(String) ctx.args.get("username");
			if (username!=null) ctx.response().setHeader("BB-USERNAME", username);
			
		    byte[] resultContent=null;
			if (BBConfiguration.getWrapResponse()){
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Wrapping the response");
				final int statusCode = result.getWrappedSimpleResult().header().status();
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Executed API: "  + ctx.request() + " , return code " + statusCode);
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Result type:"+result.getWrappedResult().getClass().getName() + " Response Content-Type:" +ctx.response().getHeaders().get("Content-Type"));
				if (ctx.response().getHeaders().get("Content-Type")!=null 
			    		&& 
			    	!ctx.response().getHeaders().get("Content-Type").contains("json")){
			    	if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("The response is a file, no wrap will be applied");
			    	return result;
			    }
			    
			    if(result.getWrappedResult() instanceof ChunkedResult<?>){
			    	return result;
			    }
			    	
				final byte[] body = JavaResultExtractor.getBody(result);
				String stringBody = new String(body, "UTF-8");
			    if (BaasBoxLogger.isTraceEnabled()) if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace ("stringBody: " +stringBody);
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
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
				}catch (Throwable e){}
			}else{ //if (BBConfiguration.getWrapResponse())
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("The response will not be wrapped due configuration parameter");
				try {
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
				}catch (Throwable e){}
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("WrapperResponse:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(result),"UTF-8"));
			}
			ctx.response().setHeader("Content-Length", Long.toString(JavaResultExtractor.getBody(result).length));
			if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
			return result;
		}); //map
	}//wrapAsync

}
