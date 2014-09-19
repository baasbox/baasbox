package com.baasbox.service.stats;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Stub service for sse connections
 * Created by Andrea Tortorella on 12/09/14.
 */
public class StatsService {


    public static JsonNode obtainMessage(String name, String type, JsonNode message) {
        return null;
    }

    public static void publish(JsonNode message){
        publish(null,message);
    }
    public static void publish(String name, JsonNode message) {
        //todo implement sse logging
    }
}
