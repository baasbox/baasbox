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

import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.google.common.collect.ImmutableMap;


public class UserCredentialWrapFilter extends Action.Simple {


	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Result tempResult=null;
		Http.Context.current.set(ctx);

		//try to use the session token
		Logger.debug("Try to authenticate via session token");
		boolean isCredentialOk=(new SessionTokenAccess()).setCredential(ctx);
		if (!isCredentialOk){
			Logger.debug("Session token not valid/not provided. Trying with basic auth");
			isCredentialOk=(new BasicAuthAccess()).setCredential(ctx);
		}
		String appcode=(String)ctx.args.get("appcode");
		Logger.debug("After authentication, appcode is: " + appcode);
	    if (appcode == null || appcode.isEmpty() || appcode.equals("null")){
	    	Logger.debug("Invalid App Code, AppCode is empty!");
	    	tempResult= badRequest("Invalid App Code. AppCode is empty or not set");
	    }else if (!isCredentialOk){
			Logger.debug("Authentication not valid/not provided");
			tempResult= unauthorized("Authentication info not valid or not provided");
		}

		//executes the request
		if (tempResult==null) tempResult = delegate.call(ctx);

		WrapResponse wr = new WrapResponse();
		Result result=wr.wrap(ctx, tempResult);
				
		Logger.trace("Method End");
	    return result;
	}

}
