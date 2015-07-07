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

package com.baasbox.service.scripting.js;

import java.io.IOException;

import jdk.nashorn.internal.runtime.ECMAErrors;
import jdk.nashorn.internal.runtime.ECMAException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 *
 * Created by Andrea Tortorella on 10/06/14.
 */
public class Api {

    public static String mainModule(){
        return ScriptingService.main();
    }

    public static String execCommand(String commandStr,JsonCallback callback){
    	BaasBoxLogger.debug("Command to execute: "+commandStr);
        try {
            JsonNode node = Json.mapper().readTree(commandStr);
            if (!node.isObject()){
                BaasBoxLogger.error("Command is not an object");
                throw ECMAErrors.typeError("Invalid command");
            }
            ObjectNode o = (ObjectNode)node;
            String main = mainModule();
            if (main!=null){
                o.put("main",main);
            }
            JsonNode exec = CommandRegistry.execute(node,callback);
            String res = exec== null?null:exec.toString();
            BaasBoxLogger.debug("Command result: "+res);
            return res;
        } catch (IOException e) {
            BaasBoxLogger.error("IoError "+ExceptionUtils.getMessage(e),e);
            throw ECMAErrors.typeError(e,"Invalid command definition");
        } catch (CommandException e){
            BaasBoxLogger.error("CommandError: "+ExceptionUtils.getMessage(e),e);
            throw new ECMAException(ExceptionUtils.getMessage(e),e);
        }
    }


    public static String currentUserName(){
        String currentUser = DbHelper.getCurrentHTTPUsername();
        return currentUser;
    }

    public static void connectAsAdmin(){
        DbHelper.reconnectAsAdmin();
    }

    public static void connectAsAuthenticatedUser(){
        DbHelper.reconnectAsAuthenticatedUser();
    }

    public static Object require(String name){
        return NashornEngine.getNashorn().require(name);
    }
}
