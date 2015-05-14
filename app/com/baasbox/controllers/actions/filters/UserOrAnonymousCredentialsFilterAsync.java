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
import play.mvc.SimpleResult;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.security.SessionKeys;
import play.core.j.JavaResultExtractor;

/**
 * This Filter checks if user credentials are present in the request and injects
 * them into the context. Otherwise, injects the internal user for anonymous
 * access
 * 
 * @author Claudio
 * 
 */
public class UserOrAnonymousCredentialsFilterAsync extends Action.Simple {

	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		F.Promise<SimpleResult> tempResult = null;
		Http.Context.current.set(ctx);
		IAccessMethod method = IAccessMethod.getAccessMethod(ctx,true);

		if (!method.isValid()) {
			tempResult = F.Promise.pure(unauthorized("Missing required or invalid authorization info"));
		}

		if (tempResult == null) {
			boolean isCredentialOk = method.setCredential(ctx);
			if (!isCredentialOk) {
				if (method.isAnonymous()){
					tempResult = F.Promise.pure(unauthorized("Missing required or invalid authorization info"));
				} else {
					tempResult = F.Promise.pure(CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus());
				}
			} else // valid credentials have been found
			// internal administrator is not allowed to access via REST
			if (((String) ctx.args.get("username"))
					.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
					|| (((String) ctx.args.get("username"))
							.equalsIgnoreCase(BBConfiguration
									.getBaasBoxUsername()) && !method.isAnonymous()))
				tempResult = F.Promise.pure(forbidden("The user " + ctx.args.get("username")
						+ " cannot access via REST"));

			// if everything is ok.....
			// executes the request
			if (tempResult == null)
				tempResult = delegate.call(ctx);
		}

		WrapResponse wr = new WrapResponse();
		return wr.wrapAsync(ctx, tempResult);
	}

}
