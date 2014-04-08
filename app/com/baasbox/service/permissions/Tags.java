package com.baasbox.service.permissions;

/**
 * Created by eto on 08/04/14.
 */
public class Tags {
    public final static String KEY ="tag";

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
