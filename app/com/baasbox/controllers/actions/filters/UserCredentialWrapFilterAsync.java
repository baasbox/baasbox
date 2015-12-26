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

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.security.SessionKeys;

import play.mvc.SimpleResult;
import play.libs.F;

public class UserCredentialWrapFilterAsync extends Action.Simple {


	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		F.Promise<SimpleResult> tempResult=null;
		Http.Context.current.set(ctx);
		String token=ctx.request().getHeader(SessionKeys.TOKEN.toString());
		if (StringUtils.isEmpty(token)) token = ctx.request().getQueryString(SessionKeys.TOKEN.toString());
		String authHeader = ctx.request().getHeader("authorization");
		boolean isCredentialOk=false;
		
		if (StringUtils.isEmpty(token) && StringUtils.isEmpty(authHeader)){
			if (!StringUtils.isEmpty(RequestHeaderHelper.getAppCode(ctx)))
				tempResult=F.Promise.<SimpleResult>pure(unauthorized("Missing both Session Token and Authorization info"));
			else   
				tempResult=F.Promise.<SimpleResult>pure(badRequest("Missing Session Token, Authorization info and even the AppCode"));
		}else if (!StringUtils.isEmpty(authHeader) && StringUtils.isEmpty(RequestHeaderHelper.getAppCode(ctx))) {
			if (Logger.isDebugEnabled()) Logger.debug("There is basic auth header, but the appcode is missing");
			if (Logger.isDebugEnabled()) Logger.debug("Invalid App Code, AppCode is empty!");
	    	tempResult= F.Promise.<SimpleResult>pure(badRequest("Invalid App Code. AppCode is empty or not set"));
		}
		
		if (tempResult == null){
			if (!StringUtils.isEmpty(token)) isCredentialOk=(new SessionTokenAccess()).setCredential(ctx);
			else isCredentialOk=(new BasicAuthAccess()).setCredential(ctx);
			
			if (!isCredentialOk){
				//tempResult= unauthorized("Authentication info not valid or not provided. HINT: is your session expired?");
				tempResult= F.Promise.<SimpleResult>pure(CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus());
			} else	{
				//internal administrator is not allowed to access via REST
				if (((String)ctx.args.get("username")){
						.equalsIgnoreCase(
								BBConfiguration.getInstance().getBaasBoxAdminUsername())
						||
						((String)ctx.args.get("username")).equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername()))
					tempResult=F.Promise.<SimpleResult>pure(forbidden("The user " +ctx.args.get("username")+ " cannot access via REST"));
				}else{
					//one last thing: is the root user that is trying to access?
					String username = (String)ctx.args.get("username");
					String password = (String)ctx.args.get("password");
					//the following check is necessary if we are using a remote connection because "root" is a valid user for ODB and we do not want to give direct access to the DB
					if (username.equals("root") && !BBConfiguration.getInstance().isRootAsAdmin()){
						tempResult=F.Promise.<SimpleResult>pure(unauthorized("User root is not authorized to access"));
					}
					//BTW if root can access as admin, we override its username
					if (BBConfiguration.getInstance().isRootAsAdmin() && username.equals("root") && password.equals(BBConfiguration.getInstance().getRootPassword())){
						//then override username and password
						ctx.args.put("username", BBConfiguration.getInstance().getBaasBoxAdminUsername());
						ctx.args.put("password", BBConfiguration.getInstance().getBaasBoxAdminPassword());
					}
				}
			} //tempResult == null
			//if everything is ok.....
			//executes the request
			if (tempResult==null) tempResult = delegate.call(ctx);
		}
		
		
		WrapResponse wr = new WrapResponse();
		return wr.wrapAsync(ctx, tempResult);
	}

}
