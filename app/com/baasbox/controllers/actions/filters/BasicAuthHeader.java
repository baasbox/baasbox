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

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.exceptions.BasicAuthException;

import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Inject the admin credentials into the args argument
 * @author claudio
 */
public class BasicAuthHeader extends Action.Simple {
    private static final String AUTHORIZATION = "authorization";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    //trick to prevent annoying browser auth popup. See: http://loudvchar.blogspot.ca/2010/11/avoiding-browser-popup-for-401.html
    public static final String REALM = "BB-Basic realm=\""+ BBConfiguration.getRealm() +"\"";

	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Logger.debug("BasicAuthHeader for resource " + Http.Context.current().request());
		String username = "";
        String password = "";
		try{
			//getting the credential from the request header: http://digitalsanctum.com/2012/06/07/basic-authentication-in-the-play-framework-using-custom-action-annotation/
			//--------------------------------------------------------------
			String authHeader = ctx.request().getHeader(AUTHORIZATION);
	        if (authHeader == null) { 
	        	throw new BasicAuthException();
	        }
	
	        String auth = authHeader.substring(6);
	        byte[] decodedAuth = new sun.misc.BASE64Decoder().decodeBuffer(auth);
	        String[] credString = new String(decodedAuth, "UTF-8").split(":");
	
	        if (credString == null || credString.length != 2) {
	        	throw new BasicAuthException();
	        }
	
	        username = credString[0];
	        password = credString[1];
		}catch (BasicAuthException e){ 
			ctx.response().setHeader(WWW_AUTHENTICATE, REALM); 
			return unauthorized();
		}
		ctx.args.put("username", username);
		ctx.args.put("password", password);
		
		Logger.trace("Method End");
		return delegate.call(ctx);
	}

}
