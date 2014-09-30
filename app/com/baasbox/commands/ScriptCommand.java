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
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface for commands executed from scripting languages.
 * Any command should return a JsonNode response or null.
 *
 * The request format of a command follows the following pattern:
 *
 * {name: "name of the command",
 *  resource: optional resource name,
 *  params: {}*,}
 *
 * Created by Andrea Tortorella on 30/06/14.
 */
public abstract interface ScriptCommand {
    public static final String NAME= "name";
    public static final String PARAMS="params";
    public static final String RESOURCE = "resource";
    public static final String ID = "mod";
    public static final String MAIN="main";

    public default void validate(JsonNode command,boolean hasParams) throws CommandParsingException {
        boolean isObj = command.isObject();
        if (!isObj)throw new CommandParsingException(command,"command is not an object");
        JsonNode res = command.get(RESOURCE);
        if (res==null||!res.isTextual())throw new CommandParsingException(command,"missing required resource");
        JsonNode name = command.get(NAME);
        if (name == null||!name.isTextual())throw new CommandParsingException(command,"missing required name");

        if (hasParams){
            JsonNode p = command.get(PARAMS);
            if (p==null||!(p.isObject())){
                throw new CommandParsingException(command,"missing required parameters");
            }
        }
    }

    public default void validate(JsonNode command) throws CommandParsingException{
        validate(command,false);
    }




    public abstract JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException;
}
