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

package com.baasbox.commands;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Internal;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.stats.StatsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Andrea Tortorella on 01/07/14.
 */
class ScriptsResource extends Resource {
    public static final Resource INSTANCE = new ScriptsResource();

    @Override
    public String name() {
        return "scripts";
    }

    @Override
    public Map<String, ScriptCommand> commands() {
        return COMMANDS;
    }


    private static  final Map<String,ScriptCommand> COMMANDS= ImmutableMap.<String,ScriptCommand>builder()
               .put("log", new ScriptCommand() {
                   @Override
                   public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                       JsonNode idOfTheModule = command.get(ScriptCommand.ID);
                       JsonNode par = command.get(ScriptCommand.PARAMS);
                       if (idOfTheModule == null) {
                           throw new CommandParsingException(command, "module id is null");
                       }
                       StatsService.publish("scripts",StatsService.obtainMessage("scripts",idOfTheModule.asText(),par));
                       return null;
                   }
               })
                .put("switchUser", new ScriptCommand() {
                    @Override
                    public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                        try {
                            DbHelper.reconnectAsAdmin();
                            return callback.call(NullNode.getInstance());
                        } finally {
                            DbHelper.reconnectAsAuthenticatedUser();
                        }
                    }
                })
                .put("storage", new ScriptCommand() {
                    @Override
                    public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                        //todo local storage
                        return storageCommand(command, callback);
                    }

                })
                .put("event", new ScriptCommand() {
                                @Override
                                public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                                    // todo publish pubic json event for streaming
                                    return null;
                                }

                })
                .build();

    private static JsonNode storageCommand(JsonNode command, JsonCallback callback) throws CommandException {
        JsonNode moduleId = command.get(ScriptCommand.ID);

        if (moduleId==null||!moduleId.isTextual()){
            throw new CommandParsingException(command,"error parsing module id");
        }
        String id = moduleId.asText();
        JsonNode params = command.get(ScriptCommand.PARAMS);
        if (params == null||!params.isObject()){
            throw new CommandParsingException(command,"error parsing params");
        }
        JsonNode actionNode = params.get("action");
        if (actionNode==null||!actionNode.isTextual()){
            throw new CommandParsingException(command,"error parsing action");
        }
        String action=actionNode.asText();
        ODocument result;
        if ("get".equals(action)){
            try {
                result = ScriptingService.getStore(id);
             } catch (ScriptException e) {
                //should never happen
                throw new CommandExecutionException(command,"script does not exists");
            }
        } else if ("set".equals(action)){
            JsonNode args = params.get("args");
            if (args==null){
                args = NullNode.getInstance();
            }
            if (!args.isObject()||!args.isNull()){
                throw new CommandExecutionException(command,"Stored values should be objects or null");
            }
            try {
                result = ScriptingService.resetStore(id,args);
            } catch (ScriptException e) {
                throw new CommandExecutionException(command,"script does not exists");
            }
        } else if ("swap".equals(action)){
            if (callback==null) throw new CommandExecutionException(command,"missing function callback");
            try {
                result = ScriptingService.swap(id,callback);
            } catch (ScriptException e) {
                throw new CommandExecutionException(command,e.getMessage(),e);
            }
        } else if ("trade".equals(action)){
            if (callback==null) throw new CommandExecutionException(command,"missing function callback");
            try {
                result = ScriptingService.trade(id,callback);
            } catch (ScriptException e) {
                throw new CommandExecutionException(command,e.getMessage(),e);
            }
        } else {
            throw new CommandParsingException(command,"unknown action: "+action);
        }
        if (result == null){
            return NullNode.getInstance();
        } else {
            String s = result.toJSON();
            try {
                JsonNode jsonNode = Json.mapper().readTree(s);
                return jsonNode;
            } catch (IOException e) {
                throw new CommandExecutionException(command,"error converting result",e);
            }
        }
    }

}
