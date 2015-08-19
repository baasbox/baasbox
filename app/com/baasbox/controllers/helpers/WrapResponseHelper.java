package com.baasbox.controllers.helpers;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;

import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;

public class WrapResponseHelper {

	public static String preludeOk(Context ctx) {
		ImmutableMap<String,String[]> requestHeaders = ImmutableMap.copyOf(ctx.request().headers());
		ImmutableMap<String,String> responseHeaders = ImmutableMap.copyOf(ctx.response().getHeaders());
		ImmutableMap<String,String[]> queryStrings = ImmutableMap.copyOf(ctx.request().queryString());
		
		return preludeOk (requestHeaders,responseHeaders,queryStrings);
	}
	
	
	public static String preludeOk(ImmutableMap<String,String[]> requestHeaders,ImmutableMap<String,String> responseHeaders,ImmutableMap<String,String[]> queryStrings) {
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append("\"result\":\"ok\",")
		.append(setCallIdOnResult(queryStrings))
		.append(setMoreField(responseHeaders))
		.append("\"data\":").toString();
	}
	
	public static String endOk(int statusCode) {
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append(",\"http_code\":")
		.append(statusCode).toString();
	}
	
	public static String setCallIdOnResult(RequestHeader request) {
		ImmutableMap<String,String[]> queryStrings = ImmutableMap.copyOf(request.queryString());
		return setCallIdOnResult(queryStrings);
	}
	
	public static String setCallIdOnResult(ImmutableMap<String,String[]> queryStrings) {
		String key = "call_id";
		String callId = queryStrings.containsKey(key) && queryStrings.get(key).length > 0 ? queryStrings.get(key)[0] : null;
		if (!StringUtils.isEmpty(callId)) return new StringBuilder("\"call_id\":\"").append(callId.replace("\"","\\\"") + "\",").toString();
		else return "";
	}
	
	private static String setMoreField(ImmutableMap<String,String> responseHeaders) {
		String more = responseHeaders.get("X-BB-MORE");
		String toRet = "";
		if (!StringUtils.isEmpty(more)) {
			toRet = new StringBuilder("\"more\":").append(more).append(",").toString();
		}
		return toRet;
	}

}
