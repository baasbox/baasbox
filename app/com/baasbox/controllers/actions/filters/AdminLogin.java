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

import com.baasbox.BBConfiguration;
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
public class AdminLogin extends Action.Simple {


	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Logger.debug("AdminLogin  for resource " + Http.Context.current().request());
		
		
		String adminUser=BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_USERNAME);
		String adminPassword = BBConfiguration.configuration.getString(IBBConfigurationKeys.ADMIN_PASSWORD);
		ctx.args.put("username", adminUser);
		ctx.args.put("password", adminPassword);
		
		Logger.trace("Method End");
		return delegate.call(ctx);
	}

}
