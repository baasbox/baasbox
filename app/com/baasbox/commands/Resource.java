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
import com.baasbox.commands.exceptions.CommandNotFoundException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.Map;

/**
 *
 * Created by Andrea Tortorella on 30/06/14.
 */
abstract class Resource {
//todo discuss better naming this behaves more as a namespace


    public abstract String name();

    public abstract Map<String,ScriptCommand> commands();


    public final JsonNode execute(JsonNode request, JsonCallback callback) throws CommandException {
        if (request == null) throw new CommandParsingException(NullNode.getInstance(),"Invalid request: null");
        JsonNode nameNode = request.get(ScriptCommand.NAME);

        if (nameNode != null && nameNode.isTextual()) {
            String name = nameNode.textValue();
            if (name==null || name.length()==0){
                throw new CommandParsingException(request,"Command name cannot be empty");
            }

            ScriptCommand command = commands().get(name);
            if (command == null){
                throw new CommandNotFoundException(request,"command not defined: "+name);
            }
            return command.execute(request,callback);
        } else {
            throw new CommandParsingException(request,"Missing command name");
        }
    }
}
