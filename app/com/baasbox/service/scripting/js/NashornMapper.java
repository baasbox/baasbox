/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.scripting.js;

import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Undefined;
import play.Logger;

import java.io.IOException;

/**
 * This implements mappings of data to/from javascript
 * Created by Andrea Tortorella on 30/06/14.
 */
final class NashornMapper {
    private final static String JSON_PARSE = "asJson";
    private final static String JSON_STRINGIFY = "toJsonString";

    private final static String EVENT_NAME="name";
    private final static String EVENT_DATA="data";

    private ScriptObjectMirror mirror;

    NashornMapper(){
    }

    void setMirror(ScriptObjectMirror mirror) {
        this.mirror = mirror;
    }


    Object convertEvent(String event, Object eventData) throws ScriptEvalException{
        // todo add support for all dataTypes
        String eventJson;
        if (eventData == null) {

            eventJson = simpleEvent(event);

        } else if (eventData instanceof String) {

            eventJson = stringEvent(event,(String)eventData);

        } else if (eventData instanceof Integer ||
                   eventData instanceof Long ||
                   eventData instanceof Double ||
                   eventData instanceof Float){

            eventJson = primitiveEvent(event,(Number)eventData);

        } else if (eventData instanceof Boolean) {

            eventJson = primitiveEvent(event,(Boolean)eventData);

        } else if (eventData instanceof JsonNode) {
            eventJson = objectEvent(event, (JsonNode) eventData);
        } else {
            throw new ScriptEvalException("Unsupported event data "+eventData.getClass());
        }

        return convertToJSJson(eventJson);
    }

    private static String stringEvent(String event,String data){
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(EVENT_NAME,event);
        node.put(EVENT_DATA,data);
        return node.toString();
    }

    private static String simpleEvent(String event){
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(EVENT_NAME,event);
        return node.toString();
    }

    private static String primitiveEvent(String event,Number data){
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(EVENT_NAME,event);
        node.put(EVENT_DATA,data.doubleValue());
        return node.toString();
    }

    private static String primitiveEvent(String event,Boolean data) {
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(EVENT_NAME,event);
        node.put(EVENT_DATA,data);
        return node.toString();
    }

    private static String objectEvent(String event,JsonNode data) {
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(EVENT_NAME,event);
        node.put(EVENT_DATA,data);
        return node.toString();
    }

    private Object convertToJSJson(String data) {
        return mirror.callMember(JSON_PARSE,data);
    }


    public ScriptResult convertResult(Object result) throws ScriptEvalException {
        if (result == null){
            return ScriptResult.NULL;
        } else if (result instanceof Boolean){
            return (Boolean) result ?ScriptResult.TRUE:ScriptResult.FALSE;
        } else if (result instanceof String){
            return new ScriptResult((String)result);
        } else if (result instanceof ScriptObjectMirror){
            ScriptObjectMirror mirror =((ScriptObjectMirror) result);
            JsonNode node = convertToJson(mirror);
            if (node != null){
                return new ScriptResult(node);
            } else {
                return ScriptResult.NULL;
            }
        } else if (result instanceof Number){
            return  new ScriptResult((Number)result);
        } else if (result instanceof Undefined){
            return ScriptResult.NULL;
        } else {
            // todo handle other mirror types
            Logger.warn("Mirror: %s, of type: %s",result,result.getClass());
        }

        return null;
    }

    private JsonNode convertToJson(ScriptObjectMirror value) throws ScriptEvalException {
        Object val = mirror.callMember(JSON_STRINGIFY,value);
        if (!(val instanceof String)){
            // this should usually mean that the returned value is a function
            // but we check for other types
            if (value.isFunction()){
                throw new ScriptEvalException("Functions cannot be returned from handlers");
            }
            return null;
        }

        try {
            return Json.mapper().readTree((String)val);
        } catch (IOException e) {
            throw new ScriptEvalException(e);
        }
    }
}
