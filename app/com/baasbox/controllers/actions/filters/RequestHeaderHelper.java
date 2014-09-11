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

package com.baasbox.controllers.actions.filters;

import play.Logger;
import play.mvc.Http.Context;

import com.baasbox.security.SessionKeys;

public class RequestHeaderHelper {
	public static String getAppCode(Context ctx){
		//first guess if the appcode is present into the request header
		String appCode=ctx.request().getHeader(SessionKeys.APP_CODE.toString());
		if (Logger.isDebugEnabled()) Logger.debug("AppCode from header: " + appCode);
		//If not, try to search into the querystring. Useful for GET on assets
		if (appCode==null || appCode.isEmpty()){
			if (Logger.isDebugEnabled()) Logger.debug("Appcode form header is empty, trying on QueryString");
			appCode=ctx.request().getQueryString(SessionKeys.APP_CODE.toString());
			if (Logger.isDebugEnabled()) Logger.debug("AppCode from queryString: " + appCode);
		}
		if (appCode==null) Logger.debug(SessionKeys.APP_CODE.toString() + " is null");
		return appCode;
	}
}
