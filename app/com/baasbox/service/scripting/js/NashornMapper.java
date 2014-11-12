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

import com.baasbox.dao.exception.ScriptException;
import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;
import jdk.nashorn.internal.runtime.Undefined;
import play.Logger;
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This implements mappings of data to/from javascript
 * Created by Andrea Tortorella on 30/06/14.
 */
final class NashornMapper {
    private final static String JSON_PARSE = "asJson";

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
            JsonNode node = convertDeepJson(result);
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

            Logger.warn("Mirror: %s, of type: %s",result,result.getClass());
            return ScriptResult.NULL;
        }

    }


    private JsonNode convertNumber(Number number){
        if (number instanceof Long||number instanceof Integer){
            return LongNode.valueOf(number.longValue());
        } else if (number instanceof Double||number instanceof Float){
            return DoubleNode.valueOf(number.doubleValue());
        } else {
            return LongNode.valueOf(number.longValue());
        }
    }

    private JsonNode convertDeepJson(Object obj) throws ScriptEvalException{
        if (obj == null){
            return NullNode.getInstance();
        } else if (obj instanceof Boolean){
            return BooleanNode.valueOf((Boolean) obj);
        } else if (obj instanceof Undefined){
            return MissingNode.getInstance();
        } else if (obj instanceof Number){
            return convertNumber((Number)obj);
        } else if (obj instanceof String){
            return TextNode.valueOf((String)obj);
        } else if (obj instanceof ScriptObjectMirror){
            return convertMirror((ScriptObjectMirror)obj);
        } else {
            return MissingNode.getInstance();
        }
    }

    private JsonNode convertMirror(ScriptObjectMirror mirror) throws ScriptEvalException{
        if (mirror == null){

            return NullNode.getInstance();
        } else if (ScriptObjectMirror.isUndefined(mirror)){
            return MissingNode.getInstance();
        } else if (mirror.isFunction()){
            return MissingNode.getInstance();
        } else if (mirror.isArray()){
            Collection<Object> values = mirror.values();
            ArrayNode node = Json.mapper().createArrayNode();
            for (Object o: values){
                JsonNode e = convertDeepJson(o);
                if (e.isMissingNode()){
                    continue;
                }
                node.add(e);
            }
            return node;
        }else if(mirror.hasMember("toJSON")){
            Object toJSON = mirror.callMember("toJSON");
            return convertDeepJson(toJSON);
        }
        else {
            ObjectNode obj = Json.mapper().createObjectNode();
            Set<Map.Entry<String, Object>> entries = mirror.entrySet();
            for (Map.Entry<String,Object> e:entries){
                Object obv = e.getValue();
                JsonNode jsonNode = convertDeepJson(obv);
                if (jsonNode.isMissingNode()){
                    continue;
                }
                obj.put(e.getKey(),jsonNode);
            }
            return obj;
        }
    }

}
