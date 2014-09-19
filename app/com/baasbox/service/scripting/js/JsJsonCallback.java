package com.baasbox.service.scripting.js;

import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;


import java.io.IOException;

/**
 * Created by Andrea Tortorella on 04/07/14.
 */
public class JsJsonCallback implements JsonCallback{

    ScriptObjectMirror callback;
    ScriptObjectMirror context;
    ScriptObjectMirror json;

    public JsJsonCallback(ScriptObjectMirror callback,ScriptObjectMirror context,ScriptObjectMirror json){
        this.callback=callback;
        this.context=context;
        this.json=json;
    }

    @Override
    public JsonNode call(JsonNode params) {
        Object result;
        if (params == null){
            Internal.log("no params");
            result =callback.call(context);
        } else {
            Internal.log("a parameter");
            Object mirrorParams=json.callMember("parse",params.toString());
            result = callback.call(context, mirrorParams);
        }
        if (result == null){
            Internal.log("null result");
            return null;
        } else {
            Internal.log("valid result");
            Object val = json.callMember("stringify", result);
            Internal.log("stringed");
            if (val instanceof String) {
                try {
                    return Json.mapper().readTree((String)val);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return null;
            }
        }
    }
}
