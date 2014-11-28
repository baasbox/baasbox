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
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by Andrea Tortorella on 30/06/14.
 */
public abstract class BaseRestResource extends Resource {

    private final ImmutableMap.Builder<String,ScriptCommand> baseCommands =
            ImmutableMap.<String, ScriptCommand>builder()
                    .put("get", new ScriptCommand() {
                        @Override
                        public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                            return get(command);
                        }
                    })
                    .put("list", new ScriptCommand() {
                        @Override
                        public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                            return list(command);
                        }
                    })
                    .put("post", new ScriptCommand() {
                        @Override
                        public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                            return post(command);
                        }
                    })
                    .put("put", new ScriptCommand() {
                        @Override
                        public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                            return put(command);
                        }
                    })
                    .put("delete", new ScriptCommand() {
                        @Override
                        public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                            return delete(command);
                        }
                    });

    private final Map<String,ScriptCommand> commands;
    BaseRestResource(){
        commands = baseCommands().build();
    }
    protected abstract JsonNode delete(JsonNode command) throws CommandException;

    protected abstract JsonNode put(JsonNode command) throws CommandException;

    protected abstract JsonNode post(JsonNode command) throws CommandException;

    protected abstract JsonNode list(JsonNode command) throws CommandException;

    protected abstract JsonNode get(JsonNode command) throws CommandException;

    protected ImmutableMap.Builder<String,ScriptCommand>  baseCommands(){
        return baseCommands;
    }


    @Override
    public final Map<String, ScriptCommand> commands() {
        return commands;
    }

}
