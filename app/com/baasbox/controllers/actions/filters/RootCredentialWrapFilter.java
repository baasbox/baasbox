/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baasbox.controllers.actions.filters;

import org.apache.commons.lang3.StringUtils;

import com.baasbox.service.logging.BaasBoxLogger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.security.SessionKeys;

/**
 * Checks if the user/pass passed via Basic Auth are of the root user.
 * Checks the correctness of the AppCode
 * Injects the internal_admin user/pass into the context
 * @author claudio tesoriero
 */
public class RootCredentialWrapFilter extends Action.Simple {

	private final static String ROOT_USER="root";
	
	@Override
	public F.Promise<SimpleResult> call(Context ctx) throws Throwable {
		F.Promise<SimpleResult> tempResult=null;
		Http.Context.current.set(ctx);
		IAccessMethod method = IAccessMethod.getAccessMethod(ctx);

		boolean isCredentialOk=false;

		if(!method.isValid()) {
			tempResult = F.Promise.pure(unauthorized("Missing required or invalid authorization info"));
		}
		
		if (tempResult == null){

			isCredentialOk = method.setCredential(ctx);
			if (!isCredentialOk){
				//tempResult= unauthorized("Authentication info not valid or not provided. HINT: is your session expired?");

				tempResult= F.Promise.<SimpleResult>pure(CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus());
			} else	if //internal administrator is not allowed to access via REST
						(((String)ctx.args.get("username")).equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
						||
						((String)ctx.args.get("username")).equalsIgnoreCase(BBConfiguration.getBaasBoxUsername())) {
				tempResult = F.Promise.<SimpleResult>pure(forbidden("The user " + ctx.args.get("username") + " cannot access via REST"));

			} else {

				//if everything is ok.....
				//let's check the root credentials
				String username=(String)ctx.args.get("username");
				String password=(String)ctx.args.get("password");
				if (!username.equals(ROOT_USER) || !password.equals(BBConfiguration.getRootPassword())){
					tempResult= F.Promise.<SimpleResult>pure(unauthorized("root username/password not valid"));
				}
				
				//let's check the appCode
				String appCode=(String)Http.Context.current().args.get("appcode");
				if (appCode==null || !appCode.equals(BBConfiguration.configuration.getString(BBConfiguration.APP_CODE)))
					tempResult=F.Promise.<SimpleResult>pure(unauthorized("Authentication info not valid or not provided: " + appCode + " is an Invalid App Code"));
				//injects the internal admin credentials in case the controller have to connect with the DB
				ctx.args.put("username", BBConfiguration.getBaasBoxAdminUsername());
				ctx.args.put("password", BBConfiguration.getBaasBoxAdminPassword());
				//executes the request
				if (tempResult==null) tempResult = delegate.call(ctx);
			}
		}
			


		WrapResponse wr = new WrapResponse();
		SimpleResult result=wr.wrap(ctx, tempResult);
		
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	    return F.Promise.<SimpleResult>pure(result);	
	}

}
