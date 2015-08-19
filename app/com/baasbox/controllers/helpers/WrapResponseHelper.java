package com.baasbox.controllers.helpers;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;

import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;

public class WrapResponseHelper {

	public static String preludeOk(Context ctx) {
		ctx.request().getQueryString("call_id");
		return preludeOk (ctx.request().getQueryString("call_id"),
				!StringUtils.isEmpty(ctx.response().getHeaders().get("X-BB-MORE")),
				ctx.response().getHeaders().get("X-BB-MORE"));
	}
	
	
	public static String preludeOk(String callId,boolean setMoreField,String moreFieldValue) {
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append("\"result\":\"ok\",")
		.append(setCallIdOnResult(callId))
		.append(setMoreField(setMoreField,moreFieldValue))
		.append("\"data\":").toString();
	}
	
	public static String endOk(int statusCode) {
		StringBuilder toReturn = new StringBuilder();
		return toReturn.append(",\"http_code\":")
		.append(statusCode).toString();
	}
	
	
	public static String setCallIdOnResult(String callId) {
		if (!StringUtils.isEmpty(callId)) return new StringBuilder("\"call_id\":\"").append(callId.replace("\"","\\\"") + "\",").toString();
		else return "";
	}
	
	private static String setMoreField(boolean set,String value) {
		String toRet = "";
		if (set) {
			toRet = new StringBuilder("\"more\":").append(value).append(",").toString();
		}
		return toRet;
	}

}
