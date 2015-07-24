package com.baasbox.service.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import com.baasbox.util.EmptyConcurrentMap;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Stub service for sse connections
 * Created by Andrea Tortorella on 12/09/14.
 */
public class EventsService {

    private static final ConcurrentMap<EventSource,EventSource> DEFAULT = new EmptyConcurrentMap<>();

    public static enum StatType{
        SCRIPT,
        ALL,
        SYSTEM_LOGGER
    }

    private final static ConcurrentMap<StatType,ConcurrentMap<EventSource,EventSource>> STATS_CHANNELS =
            new ConcurrentHashMap<>();

    public static final AtomicInteger howManyScriptLoggerListener = new AtomicInteger(0);
    
    public static void addListener(StatType channel,EventSource src){
    	STATS_CHANNELS.compute(channel,(c,listeners)->{
            if (listeners == null){
                listeners = new ConcurrentHashMap<EventSource, EventSource>();
            }

            listeners.put(src,src);
            if (channel==StatType.SCRIPT) howManyScriptLoggerListener.getAndIncrement();
            return listeners;
        });
    }

    public static void removeScriptLogListener(EventSource src){
        removeListener(StatType.SCRIPT,src);
    }

    public static void addScriptLogListener(EventSource src){
        addListener(StatType.SCRIPT,src);
    }

    public static void removeSystemLogListener(EventSource src){
        removeListener(StatType.SYSTEM_LOGGER,src);
    }

    public static void addSystemLogListener(EventSource src){
        addListener(StatType.SYSTEM_LOGGER,src);
    }
    
    /**
     * 
     * @param channel
     * @param src
     * @return true if no more listeners are present for this channel type
     */
    public static boolean removeListener(StatType channel,EventSource src){
    	
        ConcurrentMap<EventSource, EventSource> remaining = STATS_CHANNELS.computeIfPresent(channel,(ch,listeners)->{
            EventSource removed = listeners.remove(src);
            if(removed!=null){
                removed.close();
            }
            if (listeners.isEmpty()){
                return null;
            }
            return listeners;
        });
        return remaining==null||remaining.isEmpty();
    }


    public static int publish(StatType type,String message) throws IllegalArgumentException{
    	 if (type ==StatType.ALL){
             throw new IllegalArgumentException("Cannot publish on all channel");
         }
         LongAdder a= new LongAdder();

         String messageToSend=message;
         
         STATS_CHANNELS.getOrDefault(type,DEFAULT).forEach((_e,e)->{
             e.sendData(messageToSend);
             a.increment();
         });

         
         return a.intValue();
    }
    
    public static int publish(StatType type,JsonNode message) throws IllegalArgumentException{
        return publish(type,message.toString());
    }

}
