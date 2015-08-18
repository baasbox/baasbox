package com.baasbox.controllers.helpers;

import org.apache.commons.lang.StringUtils;

import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;

public class WrapResponseHelper {

	public static String preludeOk(Context ctx) {
		Request request = ctx.request();
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append("\"result\":\"ok\",")
		.append(setCallIdOnResult(request))
		.append(setMoreField(ctx))
		.append("\"data\":").toString();
	}
	
	public static String endOk(Context ctx,int statusCode) {
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append(",\"http_code\":")
		.append(statusCode).toString();
	}
	
	public static String setCallIdOnResult(RequestHeader request) {
		String callId = request.getQueryString("call_id");
		if (!StringUtils.isEmpty(callId)) return new StringBuilder("\"call_id\":\"").append(callId.replace("\"","\\\"") + "\",").toString();
		else return "";
	}
	
	private static String setMoreField(Context ctx) {
		String more = ctx.response().getHeaders().get("X-BB-MORE");
		String toRet = "";
		if (!StringUtils.isEmpty(more)) {
			toRet = new StringBuilder("\"more\":").append(more).append(",").toString();
		}
		return toRet;
	}

}
