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
import com.baasbox.dao.exception.CollectionAlreadyExistsException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.storage.CollectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by Andrea Tortorella on 04/07/14.
 */
class CollectionsResource extends Resource {
    public static final Resource INSTANCE = new CollectionsResource();

    @Override
    public String name() {
        return "collections";
    }

    @Override
    public Map<String, ScriptCommand> commands() {
        return COMMANDS;
    }


    private static Map<String,ScriptCommand> COMMANDS  =
            ImmutableMap.<String,ScriptCommand>builder()
                        .put("post", new ScriptCommand() {
                            @Override
                            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                                return createCollection(command);
                            }
                        })
                        .put("exists", new ScriptCommand() {
                            @Override
                            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                                return existsCollection(command);
                            }
                        })
                        .put("drop", new ScriptCommand() {
                            @Override
                            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                                return dropCollection(command);
                            }
                        })
                        .build();

    private static JsonNode dropCollection(JsonNode command) throws CommandException {
        checkPreconditions(command,true);
        String coll = extractCollectionName(command);
        try {
            CollectionService.drop(coll);
            return BooleanNode.getTrue();
        } catch (InvalidCollectionException e) {
            return BooleanNode.getFalse();
        } catch (Exception e){
            throw new CommandExecutionException(command,"Error dropping collection: "+e.getMessage());
        }
    }

    private static void checkPreconditions(JsonNode command,boolean nonTransactional) throws CommandExecutionException {
        if (!DbHelper.isConnectedAsAdmin(false)){
            throw new CommandExecutionException(command,"non authorized");
        }
        if (nonTransactional && DbHelper.isInTransaction()){
            throw new CommandExecutionException(command,"cannot alter collections during transaction");
        }
    }


    private static JsonNode existsCollection(JsonNode command) throws CommandException {
//        checkPreconditions(command,false);
        String coll = extractCollectionName(command);
        try {
            boolean res =CollectionService.exists(coll);
            return res? BooleanNode.getTrue():BooleanNode.getFalse();
        } catch (SqlInjectionException e) {
            throw new CommandExecutionException(command,e.getMessage());
        } catch (InvalidCollectionException e) {
            throw new CommandExecutionException(command,"Invalid collection '"+coll+"':"+e.getMessage());
        }
    }


    private static JsonNode createCollection(JsonNode command) throws CommandException{
        checkPreconditions(command,true);
        String coll = extractCollectionName(command);
        try {
            CollectionService.create(coll);
            return BooleanNode.getTrue();
        } catch (CollectionAlreadyExistsException e) {
            return BooleanNode.getFalse();
        } catch (InvalidCollectionException e){
            throw new CommandExecutionException(command,"Invalid collection name: "+e.getMessage());
        } catch (Throwable e) {
            throw new CommandExecutionException(command,"Error creating collection: "+e.getMessage());
        }
    }

    private static String extractCollectionName(JsonNode command) throws CommandParsingException {
        JsonNode node = command.get(ScriptCommand.PARAMS);
        if (!node.isTextual()){
            throw new CommandParsingException(command,"expeceted params to be the name of a collection");
        }
        return node.asText();
    }


}
