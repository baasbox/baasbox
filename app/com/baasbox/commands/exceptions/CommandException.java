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

package com.baasbox.commands.exceptions;

import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Andrea Tortorella on 30/06/14.
 */
public abstract class CommandException extends Exception {
    protected final static String TYPE_PARSE="parse";
    protected final static String TYPE_EXEC="exec";
    protected final static String TYPE_NOT_FOUND="notfound";
    protected final static String TYPE_GENERIC="generic";

    protected final static int CODE_PARSE=0;
    protected final static int CODE_EXEC=1;
    protected final static int CODE_NOT_FOUND=2;
    protected final static int CODE_GENERIC=3;

    private final static String TYPE="error";
    private final static String CODE="errorCode";
    private final static String COMMAND="command";
    private final static String MESSAGE="message";
    /*
     * {type: 'parsing|execution|notfound',
     *  command: {original command},
     *  message: "message",
     *
     * }
     */
    private CommandException(JsonNode errorDefinition,Throwable cause) {
        super(errorDefinition.toString(),cause);
    }


    private CommandException(JsonNode errorDefinition) {
        super(errorDefinition.toString());
    }

    protected CommandException(String type,int code,JsonNode command,String message){
        this(createJSON(type, code, command, message));
    }



    protected CommandException(String type,int code,JsonNode command,String message,Throwable cause){
        this(createJSON(type, code, command, message),cause);
    }

    private static JsonNode createJSON(String type, int code, JsonNode command, String message) {
        ObjectNode node = Json.mapper().createObjectNode();
        node.put(TYPE,type);
        node.put(CODE,code);
        node.put(COMMAND,command);
        node.put(MESSAGE,message);
        return node;
    }


}
