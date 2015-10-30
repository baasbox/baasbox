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
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

public class AnonymousCredentialWrapFilterAsync extends Action.Simple {


	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Context.current.set(ctx);

		if (Logger.isDebugEnabled()) Logger.debug("AnonymousLogin  for resource " + Context.current().request());
		
		String user=BBConfiguration.getInstance().getBaasBoxUsername();
		String password = BBConfiguration.getInstance().getBaasBoxPassword();
		
		//retrieve AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);
			
		ctx.args.put("username", user);
		ctx.args.put("password", password);
		ctx.args.put("appcode", appCode);
		
		if (Logger.isDebugEnabled()) Logger.debug("username (defined in conf file): " + user);
		if (Logger.isDebugEnabled()) Logger.debug("password (defined in conf file): " + password);
		if (Logger.isDebugEnabled()) Logger.debug("appcode (from header or querystring): " + appCode);
		if (Logger.isDebugEnabled()) Logger.debug("token: N/A");
		
		//executes the request
		F.Promise<SimpleResult>  tempResult = delegate.call(ctx);

		WrapResponse wr = new WrapResponse();
		//SimpleResult result=wr.wrap(ctx, tempResult);
		F.Promise<SimpleResult> result = wr.wrapAsync(ctx,tempResult);
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	    return result;
	}

}
