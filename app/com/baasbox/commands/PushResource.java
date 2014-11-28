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
import com.baasbox.service.push.PushService;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.ImmutableMap;

import java.util.*;

/**
 * Created by Andrea Tortorella on 07/08/14.
 */
class PushResource extends Resource {
    public static final PushResource INSTANCE = new PushResource();

    @Override
    public String name() {
        return "push";
    }

    @Override
    public Map<String, ScriptCommand> commands() {
        return ImmutableMap.<String,ScriptCommand>builder().
                put("send", this::sendMessage).build();
    }


    private JsonNode sendMessage(JsonNode command,JsonCallback callback) throws CommandException{
        JsonNode params = command.get(ScriptCommand.PARAMS);
        if (params == null||!params.isObject()){
            throw new CommandParsingException(command,"missing parameters");
        }

        JsonNode body = params.get("body");
        if (body == null||!body.isObject()){
            throw new CommandParsingException(command,"missing body object parameter");
        }
        JsonNode messageNode = body.get("message");
        if (messageNode==null||!messageNode.isTextual()){
            throw new CommandParsingException(command,"missing message text parameter");
        }
        String message = messageNode.asText();

        List<String> users = new ArrayList<>();
        JsonNode usersNode = params.get("to");
        if (usersNode==null|| !usersNode.isArray()){
            throw new CommandParsingException(command,"missing required to parameter");
        }
        ArrayNode usrAry = (ArrayNode)usersNode;
        usrAry.forEach(j->{
            if (j==null||!j.isTextual()) return;
            users.add(j.asText());
        });

        JsonNode profilesNode = params.get("profiles");
        List<Integer> profiles;
        if (profilesNode == null){
            profiles = Collections.singletonList(1);
        } else if (profilesNode.isArray()) {
            ArrayNode pAry = (ArrayNode)profilesNode;
            profiles = new ArrayList<>();
            pAry.forEach((j)->{
                if(j==null||!j.isIntegralNumber()) return;
                profiles.add(j.asInt());
            });
        } else {
            throw new CommandParsingException(command,"wrong profiles parameter");
        }

        boolean[] errors = new boolean[users.size()];
        PushService ps = new PushService();

        try {
            ps.send(message,users,profiles,body,errors);
            Json.ObjectMapperExt mapper = Json.mapper();
            boolean someOk = false;
            boolean someFail = false;
            for (boolean error:errors){
                if (error)someFail=true;
                else someOk=true;
            }
            if (someFail&&someOk){
                return IntNode.valueOf(1);
            } else if (someFail){
                return IntNode.valueOf(2);
            } else {
                return IntNode.valueOf(0);
            }
        } catch (Exception e) {
            throw new CommandExecutionException(command,e.getMessage(),e);
        }
    }
}
