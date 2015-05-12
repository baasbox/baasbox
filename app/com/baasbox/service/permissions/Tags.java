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
    public static boolean verifyAccess(Http.Context ctx) throws InvalidPermissionTagException, SqlInjectionException,Exception {
        if (DbHelper.isConnectedAsAdmin(true)){
            return true;
        }
        Set<String> tags = (Set<String>)ctx.args.get(KEY);
        return PermissionTagService.areAllTagsEnabled(tags);
    }

    public static enum Reserved{
        ASSETS("assets","Access to APIs for reading and for asset downloading."),
        ACCOUNT("account","Allows users to access their accounts, login and logout, modify their passwords."),
        ACCOUNT_CREATION("account.create","Allows users to create new accounts (signup via username/password)."),
        SOCIAL("social","Allows login/signup through supported social networks."),
        LOST_PASSWORD("account.lost_password","Enables the workflow to reset a password via email."),
        USERS("users","Allows to query users' profiles."),
        FRIENDSHIP("friendship","Allows to know your followers or the people you follow on the social platform within BaasBox."),
        FRIENDSHIP_CREATIONS("friendship.create","Enables the social functions of BaasBox: following/unfollowing among the users of the BaasBox server."),
        SEND("notifications.send","Allows registered users to send push notifications to other users. Administrators can always send notifications to users."),
        RECEIVE("notifications.receive","Allows apps to send device tokens they need in order to receive push notifications through BaasBox. If you disable these functions, apps wil no longer be able to register in order to receive push notifications from such server. Apps that are already registered will keep on receiving notifications."),
        STORAGE_WRITE("data.write","Allows to create new Documents."),
        STORAGE_READ("data.read","Allows to read Collections and Documents by querying them."),
        STORAGE_UPDATE("data.update","Allows to update the contents of already existing Documents."),
        STORAGE_GRANTS("data.grants","Enables the possibility to modify the ACL of Documents."),
        FILES_READ("file.read","Allows to download the Files stored in BaasBox and query them."),
        FILES_WRITE("file.write","Allows to create new Files."),
        FILES_GRANTS("file.grants","Enables the possibility to modify the ACL of Files."),
        SCRIPT_INVOKE("scripts.invoke","Allows to make calls to plugins.")
        ;
        public final String name;
        public final String description;
        
        private final static String PREFIX="baasbox.";
        private Reserved(String name,String description){
            this.name=PREFIX+name;
            this.description=description;
        }
    }
}
