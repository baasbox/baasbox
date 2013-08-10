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
import java.io.UnsupportedEncodingException;

import com.baasbox.security.SessionKeys;

import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Context;

/**
 * Inject the admin credentials into the args argument
 */
public class BasicAuthAccess  implements IAccessMethod {
    private static final String AUTHORIZATION = "authorization";
  
    @Override
	public boolean setCredential (Context ctx)  {
		Logger.trace("Method Start");
		Logger.debug("BasicAuthHeader for resource " + Http.Context.current().request());
		//retrieve AppCode
		String appcode=RequestHeaderHelper.getAppCode(ctx);
		Logger.debug(SessionKeys.APP_CODE + ": " + appcode);
		ctx.args.put("appcode", appcode);
		
		String username = ""; 
        String password = "";

		
		//getting the credential from the request header: http://digitalsanctum.com/2012/06/07/basic-authentication-in-the-play-framework-using-custom-action-annotation/
		//--------------------------------------------------------------
		String authHeader = ctx.request().getHeader(AUTHORIZATION);
        if (authHeader == null) { 
        	Logger.debug(AUTHORIZATION + " header is null or missing");
        	return false;
        }

        String auth = authHeader.substring(6);
        Logger.debug(AUTHORIZATION + ": " + auth);
        byte[] decodedAuth;
		try {
			decodedAuth = new sun.misc.BASE64Decoder().decodeBuffer(auth);
		} catch (IOException e1) {
			Logger.error("Cannot decode " + AUTHORIZATION + " header. ",e1);
			return false;
		}
        Logger.debug ("Decoded header: " + decodedAuth);
    	String[] credString;
		try {
			credString = new String(decodedAuth, "UTF-8").split(":");
		} catch (UnsupportedEncodingException e) {
			Logger.error("UTF-8 encoding not supported, really???",e);
			throw new RuntimeException("UTF-8 encoding not supported, really???",e);
		}

        if (credString == null || credString.length != 2) {
        	Logger.debug(AUTHORIZATION + " header is not valid (has not user:password pair)");
        	return false;
        }
        username = credString[0];
        password = credString[1];
       
        Logger.debug("username: " + username);
        Logger.debug("password: <hidden>");

        ctx.args.put("username", username);
		ctx.args.put("password", password);

		
		Logger.trace("Method End");
		return true;
	}

}
