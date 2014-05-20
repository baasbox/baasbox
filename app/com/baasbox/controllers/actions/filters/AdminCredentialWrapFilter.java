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
import play.mvc.SimpleResult;
import play.libs.F;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;

/**
 * Inject the admin credentials into the args argument
 * @author claudio
 */
public class AdminCredentialWrapFilter extends Action.Simple {

	@Override
	public F.Promise<SimpleResult> call(Context ctx) throws Throwable {
		F.Promise<SimpleResult> tempResult=null;
		if (Logger.isTraceEnabled())  Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (Logger.isDebugEnabled()) Logger.debug("AdminCredentialWrapFilter  for resource " + Http.Context.current().request());
		//retrieve AppCode
		String appCode=RequestHeaderHelper.getAppCode(ctx);
		//try to retrieve from querystring
		if(appCode==null){
			appCode = ctx.request().getQueryString("appcode");
		}

		String adminUser=BBConfiguration.getBaasBoxAdminUsername();
		String adminPassword = BBConfiguration.getBaasBoxAdminPassword();
		ctx.args.put("username", adminUser);
		ctx.args.put("password", adminPassword);
		ctx.args.put("appcode", appCode);
		
		if (Logger.isDebugEnabled()) Logger.debug("admin username (defined in conf file): " + adminUser);
		if (Logger.isDebugEnabled()) Logger.debug("admin password (defined in conf file): " + adminPassword);
		if (Logger.isDebugEnabled()) Logger.debug("appcode (from header): " + appCode);
		if (Logger.isDebugEnabled()) Logger.debug("token: N/A"); 
		
		if (appCode == null || appCode.isEmpty() || appCode.equals("null")){
	    	if (Logger.isDebugEnabled()) Logger.debug("Invalid App Code, AppCode is empty!");
	    	tempResult= F.Promise.<SimpleResult>pure(badRequest("Invalid App Code. AppCode is empty or not set"));
		}
		
		//executes the request
		if (tempResult==null){
			tempResult = delegate.call(ctx);
		}

		WrapResponse wr = new WrapResponse();
		SimpleResult result=wr.wrap(ctx, tempResult);
		
		
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	    return F.Promise.<SimpleResult>pure(result);
	}

}
