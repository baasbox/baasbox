package com.baasbox.service.watchers;

import com.baasbox.service.events.EventSource;

/**
 * Created by eto on 11/14/14.
 */
public class Registration {
    public final WatchKey key;
    public final EventSource source;

    Registration(WatchKey k,EventSource source){
        this.key=k;
        this.source=source;
    }
}
