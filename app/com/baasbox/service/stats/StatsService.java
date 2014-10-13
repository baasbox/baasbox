package com.baasbox.service.stats;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.EventSource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Stub service for sse connections
 * Created by Andrea Tortorella on 12/09/14.
 */
public class StatsService {

    private static final ConcurrentHashMap<EventSource,EventSource> DEFAULT = new ConcurrentHashMap<>();

    public static enum StatType{
        SCRIPT,
        ALL,
    }

    private final static ConcurrentHashMap<StatType,ConcurrentHashMap<EventSource,EventSource>> STATS_CHANNELS =
            new ConcurrentHashMap<>();

    public static void addListener(StatType channel,EventSource src){

        STATS_CHANNELS.compute(channel,(c,listeners)->{
            if (listeners == null){
                listeners = new ConcurrentHashMap<EventSource, EventSource>();
            }
            listeners.putIfAbsent(src,src);
            return listeners;
        });
    }

    public static void removeListener(StatType channel,EventSource src){
        STATS_CHANNELS.computeIfPresent(channel,(ch,listeners)->{
            listeners.remove(src);
            if (listeners.isEmpty()){
                return null;
            }
            return listeners;
        });
    }


    public static int publish(StatType type,JsonNode message) throws IllegalArgumentException{
        if (type ==StatType.ALL){
            throw new IllegalArgumentException("Cannot publish on all channel");
        }
        LongAdder a= new LongAdder();

        STATS_CHANNELS.getOrDefault(type,DEFAULT).forEach((_e,e)->{
            Logger.info("DATA: "+message.toString());
            e.sendData(message.toString());
            a.increment();
        });

        STATS_CHANNELS.getOrDefault(StatType.ALL,DEFAULT).forEach((_e,e)->{
            e.sendData(message.toString());
            a.increment();
        });
        return a.intValue();
    }

//    public static JsonNode obtainMessage(String name, String type, JsonNode message) {
//        return null;
//    }
//
//    public static void publish(JsonNode message){
//        publish(null,message);
//    }
//
//    public static void publish(String name, JsonNode message) {
//
//    }
}
