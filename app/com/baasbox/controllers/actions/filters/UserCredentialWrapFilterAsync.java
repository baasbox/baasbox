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

import com.google.common.base.Strings;
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
		Context.current.set(ctx);

		IAccessMethod method = IAccessMethod.getAccessMethod(ctx);

		boolean isCredentialOk=false;

		if (!method.isValid()) {
			tempResult = F.Promise.pure(unauthorized("Missing required or invalid authorization info"));
		}

		
		if (tempResult == null){
			isCredentialOk = method.setCredential(ctx);

			if (!isCredentialOk){
				//tempResult= unauthorized("Authentication info not valid or not provided. HINT: is your session expired?");
				tempResult= F.Promise.<SimpleResult>pure(CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus());
			} else	
				//internal administrator is not allowed to access via REST
				if (((String)ctx.args.get("username")).equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())
						||
						((String)ctx.args.get("username")).equalsIgnoreCase(BBConfiguration.getBaasBoxUsername()))
					tempResult=F.Promise.<SimpleResult>pure(forbidden("The user " +ctx.args.get("username")+ " cannot access via REST"));
			
				//if everything is ok.....
				//executes the request
				if (tempResult==null) tempResult = delegate.call(ctx);
		}
		
		
		WrapResponse wr = new WrapResponse();
		return wr.wrapAsync(ctx, tempResult);
	}

}
