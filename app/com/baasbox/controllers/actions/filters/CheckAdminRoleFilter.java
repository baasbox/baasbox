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
import java.util.List;
import java.util.Arrays;

import com.baasbox.service.logging.BaasBoxLogger;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.UserService;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.mvc.SimpleResult;
import play.libs.F;


public class CheckAdminRoleFilter extends Action.Simple{

	@Override
	public F.Promise<SimpleResult>  call(Context ctx) throws Throwable {
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		Http.Context.current.set(ctx);
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CheckAdminRole for resource " + Http.Context.current().request());
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("CheckAdminRole user: " + ctx.args.get("username"));

		OSecurityUser user= UserService.getCurrentOSecurityUser();
		Set<?> roles=user.getRoles();

		F.Promise<SimpleResult> result=null;
		if (roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()))){
			result = delegate.call(ctx);
		}else result=F.Promise.<SimpleResult>pure(forbidden("User " + ctx.args.get("username") + " is not an administrator"));
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		return result;
	}

}
