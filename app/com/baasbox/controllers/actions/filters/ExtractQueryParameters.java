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

import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;

import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.SimpleResult;
import play.libs.F;

public class ExtractQueryParameters extends Action.Simple{

	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Http.Context.current.set(ctx);		
		if (Logger.isDebugEnabled()) Logger.debug("ExtractQueryParameters for resource " + Http.Context.current().request());
		
		QueryParams qryp =QueryParams.getParamsFromQueryString(Http.Context.current().request());
		if (Logger.isDebugEnabled()) Logger.debug("ExtractQueryParameters " + qryp);
		
		ctx.args.put(IQueryParametersKeys.QUERY_PARAMETERS, qryp);
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return delegate.call(ctx);
	}

}
