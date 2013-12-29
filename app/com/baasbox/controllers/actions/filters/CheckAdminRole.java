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

import java.util.Set;

import play.Logger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;


public class CheckAdminRole extends Action.Simple{

	@Override
	public Result call(Context ctx) throws Throwable {
		Logger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		Logger.debug("CheckAdminRole for resource " + Http.Context.current().request());
		Logger.debug("CheckAdminRole user: " + ctx.args.get("username"));
		
		OUser user=DbHelper.getConnection().getUser();
		Set<ORole> roles=user.getRoles();
		
		Result result=null;
		if (roles.contains(RoleDao.getRole("admin"))){
			result = delegate.call(ctx);
		}else result=forbidden("User " + ctx.args.get("username") + " is not an administrator");
		Logger.trace("Method End");
		return result;
	}

}
