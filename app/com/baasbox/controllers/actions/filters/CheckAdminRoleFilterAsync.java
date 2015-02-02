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

import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

import java.util.Set;


public class CheckAdminRoleFilterAsync extends Action.Simple{

	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		Context.current.set(ctx);

		if (Logger.isDebugEnabled()) Logger.debug("CheckAdminRole for resource " + Context.current().request());
		if (Logger.isDebugEnabled()) Logger.debug("CheckAdminRole user: " + ctx.args.get("username"));

		return F.Promise.promise(()->{
			try {
				DbHelper.openFromContext(ctx);
				OUser user=DbHelper.getConnection().getUser();
				Set<ORole> roles=user.getRoles();
				return roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()));
			} finally {
				DbHelper.close(DbHelper.getConnection());
			}
		}).flatMap(
				isAdmin->isAdmin?
						delegate.call(ctx)
						:F.Promise.pure(forbidden("User " + ctx.args.get("username") + " is not an administrator")));

	}

}
