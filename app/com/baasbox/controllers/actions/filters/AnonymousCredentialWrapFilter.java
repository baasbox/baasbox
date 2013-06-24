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

import com.baasbox.BBConfiguration;
import com.baasbox.security.SessionKeys;

public class AnonymousCredentialWrapFilter extends Action.Simple {


	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Logger.debug("AnonymousLogin  for resource " + Http.Context.current().request());
		
		String user=BBConfiguration.getBaasBoxUsername();
		String password = BBConfiguration.getBaasBoxPassword();
		
		//retrieve AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);
			
		ctx.args.put("username", user);
		ctx.args.put("password", password);
		ctx.args.put("appcode", appCode);
		
		Logger.debug("username (defined in conf file): " + user);
		Logger.debug("password (defined in conf file): " + password);
		Logger.debug("appcode (from header or querystring): " + appCode);
		Logger.debug("token: N/A");
		
		//executes the request
		Result tempResult = delegate.call(ctx);

		WrapResponse wr = new WrapResponse();
		Result result=wr.wrap(ctx, tempResult);
				
		Logger.trace("Method End");
	    return result;
	}

}
