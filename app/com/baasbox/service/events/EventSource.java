package com.baasbox.service.events;

import play.Logger;
import play.libs.F;

import java.util.function.Consumer;

/**
 * Created by eto on 14/10/14.
 */
public class EventSource extends play.libs.EventSource {

    private final Consumer<EventSource> mHandler;

    public static EventSource source(Consumer<EventSource> handler){
        return new EventSource(handler);
    }

    private EventSource(Consumer<EventSource> handler){
        this.mHandler = handler;
    }

    @Override
    public void sendData(String s) {
        Logger.debug("SENDING");
        super.sendData(s);

    }


    @Override
    public void onConnected() {
        mHandler.accept(this);
    }

    @Override
    public void onDisconnected(F.Callback0 callback0) {
        Logger.debug("DISCONNECTING");
        super.onDisconnected(callback0);
    }

}
