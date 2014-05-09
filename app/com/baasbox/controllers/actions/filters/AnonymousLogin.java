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
import play.mvc.SimpleResult;
import play.libs.F;

public class AnonymousLogin extends Action.Simple {


	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (Logger.isDebugEnabled()) Logger.debug("AnonymousLogin  for resource " + Http.Context.current().request());
		
		
		String user=BBConfiguration.getBaasBoxUsername();
		String password = BBConfiguration.getBaasBoxPassword();

		ctx.args.put("username", user);
		ctx.args.put("password", password);
		
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return delegate.call(ctx);
	}

}
