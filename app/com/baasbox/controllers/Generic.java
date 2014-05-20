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

package com.baasbox.controllers;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;

public class Generic extends Controller{

	public static Result getOptions(String parameter){
		response().setHeader("Allow", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Origin", "*");
		if (Logger.isDebugEnabled()) Logger.debug(Json.stringify(Json.toJson(request().headers())));
		if (request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS")!=null)
			response().setHeader("Access-Control-Allow-Headers",request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS"));
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result refreshSessionToken(){
		return ok(); 
	}
	
}
