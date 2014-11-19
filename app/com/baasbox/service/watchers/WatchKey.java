package com.baasbox.service.watchers;

import com.baasbox.util.QueryParams;

/**
 * Created by eto on 11/14/14.
 */
public class WatchKey {
    private String userName;
    private String appcode;
    private String password;
    private final QueryParams params;
    public final String collection;
    private final boolean currents;
    private final long throttle;
    private final long numUpdates;

    public WatchKey(String appcode,String user,String password,
                    String collection,QueryParams params,String currents,
                    String throttle,String updates){
        this.appcode=appcode;
        this.userName=user;
        this.password=password;
        this.collection=collection;
        this.params=params;
        this.currents=currents!=null?Boolean.valueOf(currents):false;
        this.throttle=throttle!=null?Long.valueOf(throttle):-1;
        this.numUpdates=updates!=null?Long.valueOf(updates):0;
    }

    public long throttle(){
        return throttle;
    }

    public long updates(){
        return numUpdates;
    }

    public String password(){
        return password;
    }

    public String appcode(){
        return appcode;
    }

    public String user(){
        return userName;
    }
    public boolean wantsCurrent() {
        return currents;
    }
    public QueryParams params(){
        return params;
    }

    public String collection() {
        return collection;
    }
}
