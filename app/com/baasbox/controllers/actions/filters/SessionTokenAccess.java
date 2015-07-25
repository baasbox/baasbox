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

import org.apache.commons.lang.StringUtils;

import play.mvc.Http.Context;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.service.logging.BaasBoxLogger;
import com.google.common.collect.ImmutableMap;


public class SessionTokenAccess implements IAccessMethod  {

	@Override
	public boolean setCredential(Context ctx)  {
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionTokenAccess");
		//injects the user data & credential into the context
		String token=ctx.request().getHeader(SessionKeys.TOKEN.toString());
		if (StringUtils.isEmpty(token)) token = ctx.request().getQueryString(SessionKeys.TOKEN.toString());
		
		if (token!=null) {
			  if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Received session token " + token);
			  ImmutableMap<SessionKeys, ? extends Object> sessionData = SessionTokenProviderFactory.getSessionTokenProvider().getSession(token);
			  if (sessionData!=null){
				  	if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Token identified: ");
					ctx.args.put("username", sessionData.get(SessionKeys.USERNAME));
					ctx.args.put("password", sessionData.get(SessionKeys.PASSWORD));
					ctx.args.put("appcode", sessionData.get(SessionKeys.APP_CODE));
					ctx.args.put("token", token);
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("username: " + (String)sessionData.get(SessionKeys.USERNAME));
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("password: <hidden>" );
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("appcode: " + (String)sessionData.get(SessionKeys.APP_CODE));
					if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("token: " + token);
					return true;
			  }else{
				  if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Session Token unknown");
				  return false;
			  }
		}else{
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Session Token header is null");
			return false;
		}
	}

}
