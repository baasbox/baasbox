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


import org.apache.commons.lang.exception.ExceptionUtils;

import play.Logger;
import play.Play;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;


import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.exceptions.BadCredentialsException;
import com.baasbox.db.DbHelper;
import com.baasbox.db.hook.Audit;
import com.baasbox.db.hook.HooksManager;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;



/**
 * Inject the user credentials into the args argument
 * @author claudio
 */
public class ConnectToDB extends Action.Simple {
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String REALM = "BB-Basic realm=\""+ BBConfiguration.getRealm() +"\"";

	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		//set the current Context in the local thread to be used in the views: https://groups.google.com/d/msg/play-framework/QD3czEomKIs/LKLX24dOFKMJ
		Http.Context.current.set(ctx);
		
		Logger.debug("UserLogin for resource " + Http.Context.current().request());
		
		OGraphDatabase database = null;
		String username = (String) ctx.args.get("username");
		String password = (String) ctx.args.get("password");
		
		Result result=null;
		try{
			
	        try{
	        	database=DbHelper.open(username, password);
	        }catch (OSecurityAccessException e){
	        	ctx.response().setHeader(WWW_AUTHENTICATE, REALM); 
	        	throw new BadCredentialsException(e);
	        }
			
			result = delegate.call(ctx);
			
		}catch (BadCredentialsException e){
			Logger.error("UserLogin", e);
			Http.Context.current.set(ctx); 
			result = unauthorized(views.html.error.render(Http.Status.UNAUTHORIZED));
		}catch (OSecurityAccessException e){
			Logger.error("UserLogin", e);
			Http.Context.current.set(ctx); 
			result = forbidden(views.html.error.render(Http.Status.FORBIDDEN));
		}catch (Throwable e){
			Logger.error("UserLogin", e);
			if (Play.isDev()) 
				result=internalServerError(ExceptionUtils.getFullStackTrace(e));
			else 
				result=internalServerError();
		}finally{
			if (database!=null && !database.isClosed()) database.close();
		}
		Logger.trace("Method End");
		return result;
	}

}
