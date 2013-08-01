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
 * Inject the admin credentials into the args argument
 * @author claudio
 */
public class AdminCredentialWrapFilter extends Action.Simple {




	@Override
	public Result call(Context ctx) throws Throwable {
		Result tempResult=null;
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Logger.debug("AdminCredentialWrapFilter  for resource " + Http.Context.current().request());
		//retrieve AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);

		String adminUser=BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
		String adminPassword = BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);
		ctx.args.put("username", adminUser);
		ctx.args.put("password", adminPassword);
		ctx.args.put("appcode", appCode);
		
		Logger.debug("admin username (defined in conf file): " + adminUser);
		Logger.debug("admin password (defined in conf file): " + adminPassword);
		Logger.debug("appcode (from header): " + appCode);
		Logger.debug("token: N/A");
		
		if (appCode == null || appCode.isEmpty() || appCode.equals("null")){
	    	Logger.debug("Invalid App Code, AppCode is empty!");
	    	tempResult= badRequest("Invalid App Code. AppCode is empty or not set");
		}
		
		//executes the request
		if (tempResult==null){
			tempResult = delegate.call(ctx);
		}

		WrapResponse wr = new WrapResponse();
		Result result=wr.wrap(ctx, tempResult);
		
		
		Logger.trace("Method End");
	    return result;
	}

}
