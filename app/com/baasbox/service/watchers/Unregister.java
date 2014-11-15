package com.baasbox.service.watchers;

/**
 * Created by eto on 11/14/14.
 */
public class Unregister {
    public final WatchKey key;

    Unregister(WatchKey key) {
        this.key = key;
    }
}
