/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
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

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.SessionTokenAccess;
import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.events.EventsService;
import com.baasbox.service.events.EventSource;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import play.Logger;
import play.mvc.Result;

import java.util.Set;

import static play.mvc.Controller.*;
import static play.mvc.Results.ok;

/**
 * Created by eto on 13/10/14.
 */
public class EventsController {

    public static Result openLogger(){
        try {
            if (Logger.isTraceEnabled())Logger.trace("Method start");

            SessionTokenAccess sessionTokenAccess = new SessionTokenAccess();
            boolean okCredentials = sessionTokenAccess.setCredential(ctx());
            if (!okCredentials) {
                return CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus();
            } else {
                String username = (String) ctx().args.get("username");
                if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername()) ||
                        username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())) {
                    return forbidden("The user " + username + " cannot acces via REST");
                }
            }
            String appcode = (String) ctx().args.get("appcode");
            String username = (String) ctx().args.get("username");
            String password = (String) ctx().args.get("password");
            try {
                DbHelper.open(appcode, username, password);
                OUser user = DbHelper.getConnection().getUser();
                Set<ORole> roles = user.getRoles();
                if (!roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()))) {
                    return forbidden("Logs can only be read by administrators");
                }
            } catch (InvalidAppCodeException e) {
                return badRequest(e.getMessage());
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }

            response().setContentType("text/event-stream");
            return ok(EventSource.source((eventSource) -> {

                eventSource.onDisconnected(() -> EventsService.removeLogListener(eventSource));
                EventsService.addLogListener(eventSource);

            }));
        } finally {
            if (Logger.isTraceEnabled()) Logger.trace("Method end");
        }
    }
}
