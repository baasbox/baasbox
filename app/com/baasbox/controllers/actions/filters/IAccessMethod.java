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

import com.baasbox.security.SessionKeys;
import com.google.common.base.Strings;
import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Context;

public interface IAccessMethod {

	boolean setCredential(Context ctx);

	default boolean isValid(){
		return true;
	}

	default boolean isAnonymous(){return false;}

	static IAccessMethod getAccessMethod(Context ctx,boolean allowAnonymousAccess){
		Http.Request request = ctx.request();
		String bbtoken = request.getHeader(SessionKeys.TOKEN.toString());
		if (Strings.isNullOrEmpty(bbtoken)) {
			bbtoken = request.getQueryString(SessionKeys.TOKEN.toString());
		}

		if (bbtoken != null) {
			if (Logger.isDebugEnabled()) Logger.debug("Session token strategy selected");
			return JWTAccessMethod.INSTANCE;
		}

		String auth = request.getHeader("authorization");
		String appcode = RequestHeaderHelper.getAppCode(ctx);
		boolean missingAuth = Strings.isNullOrEmpty(auth);
		boolean missingAppcode = Strings.isNullOrEmpty(appcode);
		if (missingAuth){
			if (missingAppcode){
				if (Logger.isDebugEnabled()){
					Logger.debug("No auth strategy is available");
					Logger.debug("There is basic auth header but the appcode is missing");
					Logger.debug("Invalid App Code, AppCode is empty");
				}
				return InvalidAccessMethod.INSTANCE;
			} else if (allowAnonymousAccess){
				return AnonymousAccessMethod.INSTANCE;
			}
		}
		if (auth.startsWith("Basic ")){
			if (Logger.isDebugEnabled())Logger.debug("Basic auth strategy selected");
			return BasicAuthAccess.INSTANCE;
		} else {
			if (Logger.isDebugEnabled())Logger.debug("JWT token strategy selected");
			return JWTAccessMethod.INSTANCE;
		}

		//return auth.startsWith("Basic ")?BasicAuthAccess.INSTANCE:JWTAccessMethod.INSTANCE;

	}

	static IAccessMethod getAccessMethod(Context ctx){
		return getAccessMethod(ctx,false);
	}

}