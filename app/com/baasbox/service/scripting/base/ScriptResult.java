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

package com.baasbox.service.scripting.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 *
 * Created by Andrea Tortorella on 10/06/14.
 */
public final class ScriptResult {
    public static final ScriptResult NULL = new ScriptResult();
    public static final ScriptResult TRUE = new ScriptResult(true,Type.BOOL);
    public static final ScriptResult FALSE = new ScriptResult(false,Type.BOOL);

    private final Object data;
    public final Type type;

    private ScriptResult(){
        this.data=null;
        this.type=Type.NULL;
    }

    private ScriptResult(Object data,Type type){
        this.data = data;
        this.type=type;
    }

    public ScriptResult(JsonNode node){
        data = node;
        type=node.isArray()?Type.ARRAY:Type.OBJECT;
    }

    public ScriptResult(String text){
        data = text;
        type=Type.TEXT;
    }

    public ScriptResult(Number number){
        data=number;
        type=Type.NUMBER;
    }

    public Object data(){
        return data;
    }

    public int status(){
        if (!Type.OBJECT.equals(type))return 200;
        JsonNode node = (JsonNode)data;
        JsonNode status = node.get("status");
        if (status==null||!status.isNumber()) return 200;
        return status.asInt();
    }

    public JsonNode content(){
        if (!Type.OBJECT.equals(type))return TextNode.valueOf("");
        JsonNode node = (JsonNode)data;
        JsonNode content = node.get("content");
        if (content==null) return TextNode.valueOf("");
        return content;
    }


    public ScriptStatus toScriptStatus() throws ScriptEvalException{
        if (type == Type.NULL|| this == TRUE){
            return ScriptStatus.ok();
        } else if (this == FALSE){
            return ScriptStatus.fail();
        } else if (type == Type.OBJECT){
            JsonNode data = (JsonNode)this.data;
            JsonNode status = data.get("status");
            if (status ==null||!status.isBoolean()){
                throw new ScriptEvalException("Script failure: unexpected result, missing status");
            }

            JsonNode message = data.get("message");
            String msg;
            if (message != null && message.isTextual()){
                msg = message.asText();
            } else {
                msg = "";
            }

            if(status.asBoolean()){
                return ScriptStatus.ok(msg);
            } else {
                return ScriptStatus.fail(msg);
            }
        }
        throw new ScriptEvalException("Script failure: unexpected result type");
    }


    public JsonNode jsonData(){
        if (type == Type.OBJECT){
            return (JsonNode)data;
        }
        throw new IllegalStateException("Expected data should be json");
    }

    public boolean isFalsy() {
        if (type == Type.NULL|| this == FALSE){
            return false;
        }
        return true;
    }


    public static enum Type{
        NULL,
        BOOL,
        TEXT,
        NUMBER,
        OBJECT,
        BINARY, //todo fix binary results
        ARRAY
    }

    @Override
    public String toString() {
        return "ScriptResult [Type: "+type+" Data: "+(data==null?"null":data)+"]";
    }
}
