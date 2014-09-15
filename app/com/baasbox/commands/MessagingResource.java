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
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.push.providers.PushNotInitializedException;
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Andrea Tortorella on 07/08/14.
 */
class MessagingResource extends Resource {
    public static final MessagingResource INSTANCE = new MessagingResource();

    @Override
    public String name() {
        return "messaging";
    }

    @Override
    public Map<String, ScriptCommand> commands() {
        return ImmutableMap.<String,ScriptCommand>builder().
                put("send", new ScriptCommand() {
                    @Override
                    public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                        return sendMessageTo(command);
                    }
                }).build();
    }

    private JsonNode sendMessageTo(JsonNode command) throws CommandException{
        JsonNode jsonNode = command.get(ScriptCommand.PARAMS);
        if (jsonNode==null||!jsonNode.isObject()){
            throw new CommandParsingException(command,"missing parameters");
        }
        JsonNode to = jsonNode.get("to");
        JsonNode content= jsonNode.get("body");
        if (to==null||content==null){
            throw new CommandParsingException(command,"missing required parameters");
        }
        PushService ps = new PushService();
        if (to.isTextual()&& content.isObject()){
            JsonNode message = content.get("message");
            if (message.isTextual()){
                try {
                    ps.send(message.asText(),to.asText());
                    return BooleanNode.getTrue();
                } catch (PushNotInitializedException e) {
                    throw new CommandExecutionException(command,"push service has not been enabled");
                } catch (UserNotFoundException e) {
                    throw new CommandExecutionException(command,"user: "+to.asText()+"not found");
                } catch (SqlInjectionException e) {
                    throw new CommandExecutionException(command,e.getMessage(),e);
                } catch (IOException e) {
                    throw new CommandExecutionException(command,e.getMessage(),e);
                }
            }
        }
        return BooleanNode.getFalse();
    }
}
