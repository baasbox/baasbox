package com.baasbox.commands;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by eto on 30/09/14.
 */
public final class ScriptCommands {
    private ScriptCommands(){}

        public static ObjectNode createCommand(String resource,String name){
        ObjectNode cmd = BBJson.mapper().createObjectNode();
        cmd.put(ScriptCommand.RESOURCE,resource);
        cmd.put(ScriptCommand.NAME,name);
        return cmd;
    }

    public static ObjectNode createCommand(String resource,String name,JsonNode params){
        ObjectNode cmd = BBJson.mapper().createObjectNode();
        cmd.put(ScriptCommand.RESOURCE,resource);
        cmd.put(ScriptCommand.NAME,name);
        cmd.put(ScriptCommand.PARAMS,params);
        return cmd;
    }

    public static ObjectNode createCommandForScript(String resource,String name,String script){
        ObjectNode cmd = BBJson.mapper().createObjectNode();
        cmd.put(ScriptCommand.RESOURCE,resource);
        cmd.put(ScriptCommand.NAME,name);
        cmd.put(ScriptCommand.ID,script);
        return cmd;
    }


    public static ObjectNode createCommandForScript(String resource,String name,String script,ObjectNode params){
        ObjectNode cmd = BBJson.mapper().createObjectNode();
        cmd.put(ScriptCommand.RESOURCE,resource);
        cmd.put(ScriptCommand.NAME,name);
        cmd.put(ScriptCommand.ID,script);
        cmd.put(ScriptCommand.PARAMS,params);
        return cmd;
    }
}
