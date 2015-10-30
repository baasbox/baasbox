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

import com.baasbox.service.logging.BaasBoxLogger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.SimpleResult;
import play.libs.F;

import com.baasbox.BBConfiguration;
import com.baasbox.security.SessionKeys;

public class AnonymousCredentialWrapFilter extends Action.Simple {


	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("AnonymousLogin  for resource " + Http.Context.current().request());
		
		String user=BBConfiguration.getInstance().getBaasBoxUsername();
		String password = BBConfiguration.getInstance().getBaasBoxPassword();
		
		//retrieve AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);
			
		ctx.args.put("username", user);
		ctx.args.put("password", password);
		ctx.args.put("appcode", appCode);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("username (defined in conf file): " + user);
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("password (defined in conf file): " + password);
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("appcode (from header or querystring): " + appCode);
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("token: N/A");
		
		//executes the request
		F.Promise<SimpleResult>  tempResult = delegate.call(ctx);

		WrapResponse wr = new WrapResponse();
		SimpleResult result=wr.wrap(ctx, tempResult);
				
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	    return F.Promise.<SimpleResult>pure(result);
	}

}
