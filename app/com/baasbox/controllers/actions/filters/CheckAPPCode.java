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

import org.slf4j.MDC;

import com.baasbox.IBBConfigurationKeys;

import play.Configuration;
import play.Logger;
import play.Play;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Inject the admin credentials into the args argument
 * @author claudio
 */
public class CheckAPPCode extends Action.Simple {

	private final String X_HEADER = "X-BAASBOX-APPCODE";

	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		String codeSent[] = ctx.request().headers().get(X_HEADER);
		
		Logger.debug("CheckAPPCode for resource " + Http.Context.current().request());
		Logger.debug("codeSent: " + ((codeSent==null)?"null":codeSent.toString()));
		Result result = badRequest("Invalid App Code: " + ((codeSent==null)?"null":codeSent[0]));

		String codeExpected = Play.application().configuration().getString(IBBConfigurationKeys.APP_CODE);
		if (codeSent!=null && codeSent.length>0 && codeSent[0].equals(codeExpected)){	
			MDC.put("appid", codeSent[0]);
			result = delegate.call(ctx);
		}
		Logger.trace("Method End");
		ctx.response().setHeader("Access-Control-Allow-Origin", "*");
		return result;
	}

}
