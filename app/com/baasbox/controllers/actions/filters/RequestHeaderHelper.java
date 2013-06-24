package com.baasbox.controllers.actions.filters;

import play.Logger;
import play.mvc.Http.Context;

import com.baasbox.security.SessionKeys;

public class RequestHeaderHelper {
	public static String getAppCode(Context ctx){
		//first guess if the appcode is present into the request header
		String appCode=ctx.request().getHeader(SessionKeys.APP_CODE.toString());
		Logger.debug("AppCode from header: " + appCode);
		//If not, try to search into the querystring. Useful for GET on assets
		if (appCode==null || appCode.isEmpty()){
			Logger.debug("Appcode form header is empty, trying on QueryString");
			appCode=ctx.request().getQueryString(SessionKeys.APP_CODE.toString());
			Logger.debug("AppCode from queryString: " + appCode);
		}
		if (appCode==null) Logger.warn(SessionKeys.APP_CODE.toString() + " is null");
		return appCode;
	}
}
