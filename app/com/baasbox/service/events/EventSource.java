package com.baasbox.service.events;

import com.baasbox.service.logging.BaasBoxLogger;
import play.libs.F;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by eto on 14/10/14.
 */
public class EventSource extends play.libs.EventSource {

	public static AtomicInteger cont = new AtomicInteger(0);
	public int id=0;
	
    private final Consumer<EventSource> mHandler;

    public static EventSource source(Consumer<EventSource> handler){
        return new EventSource(handler);
    }

    private EventSource(Consumer<EventSource> handler){
        this.mHandler = handler;
    }
    
    @Override
    public void sendData(String s) {
        BaasBoxLogger.debug("EventSource: SENDING: "+id);
        super.sendData(s);
    }


    @Override
    public void onConnected() {
    	this.id= cont.getAndIncrement();
    	BaasBoxLogger.debug("EventSource: Connecting : " + id);
        mHandler.accept(this);
    }

    @Override
    public void onDisconnected(F.Callback0 callback0) {
        super.onDisconnected(callback0);
        //Logger.debug("EventSource: DISCONNECTING: " + id);
    }

}
