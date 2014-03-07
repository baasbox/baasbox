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
import com.baasbox.IBBConfigurationKeys;

/**
 * Checks if the user/pass passed via Basic Auth are of the root user.
 * Checks the correctness of the AppCode
 * Injects the internal_admin user/pass into the context
 * @author claudio tesoriero
 */
public class RootCredentialWrapFilter extends Action.Simple {

	private final static String ROOT_USER="root";
	
	@Override
	public Result call(Context ctx) throws Throwable {
		Result tempResult=null;
		if (Logger.isTraceEnabled())  Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (Logger.isDebugEnabled()) Logger.debug("RootCredentialWrapFilter  for resource " + Http.Context.current().request());
		
		//retrieves and checks the AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);
		//try to retrieve from querystring
		if(appCode==null){
			appCode = ctx.request().getQueryString("appcode");
		}
		if (appCode == null || appCode.isEmpty() || appCode.equals("null")){
	    	if (Logger.isDebugEnabled()) Logger.debug("Invalid App Code, AppCode is empty!");
	    	tempResult= badRequest("Invalid App Code. AppCode is empty or not set");
		}else if (!appCode.equals(BBConfiguration.getAPPCODE())) {
			tempResult= badRequest("Invalid App Code.");
		}else if (BBConfiguration.getRootPassword()==null){
			tempResult=forbidden("root access is disabled");
		}else if (!(new BasicAuthAccess().setCredential(ctx))){ //retrieve the credentials
			tempResult=badRequest("No root user/password found into the request");
		}else{
			//checks the root credential. User and password must be present into the HTTP context.
			String username=(String)ctx.args.get("username");
			String password=(String)ctx.args.get("password");
			if (!username.equals(ROOT_USER) || !password.equals(BBConfiguration.getRootPassword())){
				tempResult= unauthorized("root username/password not valid");
			}
		}
	
		//executes the request
		if (tempResult==null){
			//injects the internal admin credentials in case the controller have to connect with the DB
			ctx.args.put("username", BBConfiguration.getBaasBoxAdminUsername());
			ctx.args.put("password", BBConfiguration.getBaasBoxAdminPassword());
			ctx.args.put("appcode",  appCode);
			tempResult = delegate.call(ctx);
		}

		WrapResponse wr = new WrapResponse();
		Result result=wr.wrap(ctx, tempResult);
		
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	    return result;
	}

}
