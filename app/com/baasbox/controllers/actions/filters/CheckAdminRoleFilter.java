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
import com.baasbox.service.user.UserService;

import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;


public class CheckAdminRoleFilter extends Action.Simple{

	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CheckAdminRole for resource " + Http.Context.current().request());
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CheckAdminRole user: " + ctx.args.get("username"));
		
		F.Promise<SimpleResult> result=null;
		if (UserService.isAnAdmin(ctx.args.get("username").toString())){
			result = delegate.call(ctx);
		}else {
			result=F.Promise.<SimpleResult>pure(forbidden("User " + ctx.args.get("username") + " is not an administrator"));
		}
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return result;
	}

}
