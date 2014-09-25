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
import com.baasbox.commands.exceptions.ResourceNotFoundException;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by Andrea Tortorella on 30/06/14.
 */

public class CommandRegistry {
//todo this will grow
// we should use something like annotations
// to define what is a resource and a command
// and register it in the command registry.
// this code would run anyway at most once.

    private static final Map<String,Resource> RESOURCES =
            ImmutableMap.<String,Resource>builder()
                        .put(DocumentsResource.INSTANCE.name(),     DocumentsResource.INSTANCE)
                        .put(ScriptsResource.INSTANCE.name(),       ScriptsResource.INSTANCE)
                        .put(UsersResource.INSTANCE.name(),         UsersResource.INSTANCE)
                        .put(CollectionsResource.INSTANCE.name(),   CollectionsResource.INSTANCE)
                        .put(PushResource.INSTANCE.name(), PushResource.INSTANCE)
                        .put(DBResource.INSTANCE.name(),DBResource.INSTANCE)
                        .build();



    public static JsonNode execute(JsonNode request, JsonCallback callback) throws CommandException {
        if (request == null) throw new CommandParsingException(NullNode.getInstance(),"command is null");
        JsonNode resourceNode = request.get(ScriptCommand.RESOURCE);
        if (resourceNode != null && resourceNode.isTextual()) {
            String name = resourceNode.textValue();
            if (name == null|| name.length()==0){
                throw new CommandParsingException(request,"Resource name cannot be empty");
            }
            Resource resource = RESOURCES.get(name);

            if (resource == null){
                throw new ResourceNotFoundException(request,"Resource not found");
            }
            JsonNode response = resource.execute(request,callback);

            return response;
        } else {
            throw new CommandParsingException(request,"Missing resource name");
        }
    }


}
