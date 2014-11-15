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

package com.baasbox.service.permissions;

import com.baasbox.dao.exception.InvalidPermissionTagException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import play.mvc.Http;

import java.util.Set;

/**
 * Tags contains utility methods for permission check
 * and default baasbox endpoints.
 * Created by Andrea Tortorella on 08/04/14.
 */
public class Tags {
    public final static String KEY ="tag";

    /**
     * Verifies if the current endpoint is enabled
     * @param ctx
     * @return
     * @throws InvalidPermissionTagException
     * @throws SqlInjectionException
     */
    public static boolean verifyAccess(Http.Context ctx) throws InvalidPermissionTagException, SqlInjectionException {
        if (DbHelper.isConnectedAsAdmin(true)){
            return true;
        }
        Set<String> tags = (Set<String>)ctx.args.get(KEY);
        return PermissionTagService.areAllTagsEnabled(tags);
    }

    public static enum Reserved{
        ASSETS("assets"),
        ACCOUNT("account"),
        ACCOUNT_CREATION("account.create"),
        SOCIAL("social"),
        LOST_PASSWORD("account.lost_password"),
        USERS("users"),
        FRIENDSHIP("friendship"),
        FRIENDSHIP_CREATIONS("friendship.create"),
        SEND("notifications.send"),
        RECEIVE("notifications.receive"),
        STORAGE_WRITE("data.write"),
        STORAGE_READ("data.read"),
        STORAGE_UPDATE("data.update"),
        STORAGE_GRANTS("data.grants"),
        FILES_READ("file.read"),
        FILES_WRITE("file.write"),
        FILES_GRANTS("file.grants"),
        SCRIPT_INVOKE("scripts.invoke")
        ;
        public final String name;
        private final static String PREFIX="baasbox.";
        private Reserved(String name){
            this.name=PREFIX+name;
        }
    }
}
