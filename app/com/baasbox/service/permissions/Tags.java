package com.baasbox.service.permissions;

import com.baasbox.dao.exception.InvalidPermissionTagException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import play.mvc.Http;

import java.util.Set;

/**
 * Created by eto on 08/04/14.
 */
public class Tags {
    public final static String KEY ="tag";

    public static boolean verifyAccess(Http.Context ctx) throws InvalidPermissionTagException, SqlInjectionException {
        if (DbHelper.isConnectedAsAdmin(true)){
            return true;
        }
        Set<String> tags = (Set<String>)ctx.args.get(KEY);
        return PermissionTagService.areAllTagsEnabled(tags);
    }

    public static enum Reserved{
        ASSETS("assets"),
        STORAGE("storage"),
        SEND("send_notifications"),
        RECEIVE("receive_notifications"),
        FILES("files"),
        GRANTS("grants"),
        SOCIAL("social")
        ;
        public final String name;
        private final static String PREFIX="baasbox.";
        private Reserved(String name){
            this.name=PREFIX+name;
        }
    }
}
