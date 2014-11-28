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
import com.baasbox.controllers.actions.exceptions.RidNotFoundException;
import com.baasbox.dao.exception.*;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.AclNotValidException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSecurityException;
import com.orientechnologies.orient.core.record.impl.ODocument;

import org.apache.commons.lang.exception.ExceptionUtils;

import play.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {resource: 'documents',
 *  name: <'get'|'list'|'post'|'put'|'delete',
 *  params; {
 *      collection: <collection>,
 *
 *      query: <query>,*
 *
 *      id: <id>*,
 *
 *      grants: {}
 *  }}
 *
 *
 *  {resource: 'documents',
 *   name: 'get',
 *   params: {
 *       collection: <collection>,
 *       id: <uuid>,
 *
 *   }}
 *
 *
 *  {resource: 'documents',
 *   name: 'grant',
 *   params: {
 *       collection: <collection>,
 *       id: <uuid>,
 *       users: {read: [,,,],
 *               update: [,,,,],
 *               delete: [....],
 *               all: [,,,,],
 *               },
 *       roles: {read: [,,,,],
 *               update: [,,,,],
 *               delete: [,,,,,],
 *               all: [,,,,,]
 *   }}
 *
 *
 *
 * Created by Andrea Tortorella on 30/06/14.
 */
class DocumentsResource extends BaseRestResource {
    private static final Predicate<BaasBoxPrivateFields> removeVisible = ((Predicate<BaasBoxPrivateFields>)BaasBoxPrivateFields::isVisibleByTheClient).negate();

    private static final Collection<String> TO_REMOVE = EnumSet.allOf(BaasBoxPrivateFields.class)
                                                               .stream().filter(removeVisible)
                                                                        .map(BaasBoxPrivateFields::toString)
                                                                        .collect(Collectors.toSet());


    public static final Resource INSTANCE = new DocumentsResource();

    private static final String RESOURCE_NAME = "documents";

    private static final String COLLECTION = "collection";
    private static final String QUERY = "query";
    private static final String DATA = "data";
    private static final String AUTHOR = "author";

    @Override
    protected ImmutableMap.Builder<String, ScriptCommand> baseCommands() {
        return super.baseCommands().put("grant", new ScriptCommand() {
            @Override
            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                return grant(command,true);
            }
        }).put("revoke", new ScriptCommand() {
            @Override
            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                return grant(command,false);
            }
        });
    }

    private JsonNode grant(JsonNode command, boolean grant) throws CommandException {
        validateHasParams(command);
        String coll = getCollectionName(command);
        String id = getDocumentId(command);

        try {
            try {
                String rid = DocumentService.getRidByString(id, true);
                alterGrants(command, coll, rid, true, grant);
                alterGrants(command, coll, rid, false, grant);
            } catch (Exception e){
                Logger.error("error",e);
                throw  e;
            }
        } catch (UserNotFoundException e) {

            throw new CommandExecutionException(command,"user not found exception");
        } catch (DocumentNotFoundException e) {
            throw new CommandExecutionException(command,"document not found exception");
        } catch (InvalidCollectionException e) {
            throw new CommandExecutionException(command,"invalid colleciton exception");
        } catch (InvalidModelException e) {
            throw new CommandExecutionException(command,"invalid model exception");
        } catch (RoleNotFoundException e) {
            throw new CommandExecutionException(command,"role not found exception");
        } catch (RidNotFoundException e) {
            throw new CommandExecutionException(command,"document "+id+" not found");
        }
        return BooleanNode.getTrue();
    }

    @Override
    protected JsonNode delete(JsonNode command) throws CommandException {
        validateHasParams(command);
        String coll = getCollectionName(command);
        String id = getDocumentId(command);
        String rid;
        try {
            rid = DocumentService.getRidByString(id,true);
        } catch (RidNotFoundException e) {
            return null;
        }
        try {
            DocumentService.delete(coll,rid);
        } catch (OSecurityException e) {
            throw new CommandExecutionException(command, "you don't have permissions to delete: "+id);
        } catch (ODatabaseException e){
            return null;
        } catch (Throwable e){
            throw new CommandExecutionException(command,"error executing delete command on "+id+ " message: "+e.getMessage());
        }
        return null;
    }


    private void validateHasParams(JsonNode commnand) throws CommandParsingException{
        if (!commnand.has(ScriptCommand.PARAMS)) {
            throw new CommandParsingException(commnand,"missing parameters");
        }
    }


    private String getCollectionName(JsonNode command) throws CommandParsingException {
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode collNode = params.get(COLLECTION);
        if (collNode == null|| !collNode.isTextual()){
            throw new CommandParsingException(command,"invalid collection param: "+(collNode==null?"null":collNode.toString()));
        }
        return collNode.asText();
    }


    @Override
    protected JsonNode put(JsonNode command) throws CommandException{
        validateHasParams(command);
        String coll = getCollectionName(command);
        JsonNode data = getData(command);
        String id = getDocumentId(command);
        try {
            String rid = DocumentService.getRidByString(id, true);
            ODocument doc = DocumentService.update(coll, rid, (ObjectNode)data);
            String json = JSONFormats.prepareDocToJson(doc, JSONFormats.Formats.DOCUMENT_PUBLIC);
            ObjectNode node = (ObjectNode)Json.mapper().readTree(json);
            node.remove(TO_REMOVE);
            node.remove("@rid");
            return node;
        } catch (RidNotFoundException e) {
            throw new CommandExecutionException(command,"document: "+id+" does not exists");
        } catch (UpdateOldVersionException e) {
            throw new CommandExecutionException(command,"document: "+id+" has a more recent version");
        } catch (DocumentNotFoundException e) {
            throw new CommandExecutionException(command,"document: "+id+" does not exists");
        } catch (InvalidCollectionException e) {
            throw new CommandExecutionException(command,"invalid collection: "+coll);
        } catch (InvalidModelException e) {
            throw new CommandExecutionException(command,"error updating document: "+id+" message: "+e.getMessage());
        } catch (JsonProcessingException e) {
            throw new CommandExecutionException(command,"data do not represents a valid document, message: "+e.getMessage());
        } catch (IOException e) {
            throw new CommandExecutionException(command,"error updating document: "+id+" message:"+e.getMessage());
		} catch (AclNotValidException e) {
			 throw new CommandExecutionException(command,"error updating document: "+id+" message:"+e.getMessage());
		}
    }

    @Override
    protected JsonNode post(JsonNode command) throws CommandException{
        validateHasParams(command);
        String collection = getCollectionName(command);
        JsonNode data = getData(command);
        String authorOverride = getAuthorOverride(command);

        try {
           ODocument doc =
                    DocumentService.create(collection, (ObjectNode)data);
//                    DocumentService.createOnBehalf(collection,authorOverride,data);
            if (doc == null){
                return null;
            }
            String fmt = JSONFormats.prepareDocToJson(doc, JSONFormats.Formats.DOCUMENT_PUBLIC);
            JsonNode node = Json.mapper().readTree(fmt);
            ObjectNode n =(ObjectNode)node;
            n.remove(TO_REMOVE).remove("@rid");
//            n.remove("@rid");
            return n;
        } catch (InvalidCollectionException throwable) {
            throw new CommandExecutionException(command,"invalid collection: "+collection);
        } catch (InvalidModelException e) {
            throw new CommandExecutionException(command,"error creating document: "+e.getMessage());
        } catch (Throwable e) {
            throw new CommandExecutionException(command,"error creating document: "+e.getMessage());
        }
    }

    private String getAuthorOverride(JsonNode command) throws CommandParsingException{
        JsonNode node = command.get(ScriptCommand.PARAMS).get(AUTHOR);
        if (node != null && !node.isTextual()){
            throw new CommandParsingException(command,"author must be a string");

        } else if (node != null){
            return node.asText();
        }
        return null;
    }

    private JsonNode getData(JsonNode command) throws CommandParsingException {
        JsonNode node = command.get(ScriptCommand.PARAMS).get(DATA);
        if (node == null||(!node.isObject())){
            throw new CommandParsingException(command,"missing required data parameter");
        }
        return node;
    }

    @Override
    protected JsonNode list(JsonNode command) throws CommandException {
        String collection= getCollectionName(command);
        QueryParams params = QueryParams.getParamsFromJson(command.get(ScriptCommand.PARAMS).get(QUERY));

        try {
            List<ODocument> docs = DocumentService.getDocuments(collection, params);

            String s = JSONFormats.prepareDocToJson(docs, JSONFormats.Formats.DOCUMENT_PUBLIC);
            ArrayNode lst = (ArrayNode)Json.mapper().readTree(s);
            lst.forEach((j)->((ObjectNode)j).remove(TO_REMOVE).remove("@rid"));
            return lst;
        } catch (SqlInjectionException | IOException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage(),e);
        } catch (InvalidCollectionException e) {
            throw new CommandExecutionException(command,"invalid collection: "+collection,e);
        }
    }


    @Override
    protected JsonNode get(JsonNode command) throws CommandException{
        String collection = getCollectionName(command);
        String id = getDocumentId(command);
        try {
            String rid = DocumentService.getRidByString(id, true);
            ODocument document = DocumentService.get(collection, rid);
            if (document == null){
                return null;
            } else {
                String s = JSONFormats.prepareDocToJson(document, JSONFormats.Formats.DOCUMENT_PUBLIC);
                ObjectNode node = (ObjectNode)Json.mapper().readTree(s);
                node.remove(TO_REMOVE).remove("@rid");
                return node;
            }
        } catch (RidNotFoundException e) {
            return null;
        } catch (DocumentNotFoundException e) {
            return null;
        } catch (InvalidCollectionException e) {
            throw new CommandExecutionException(command,"invalid collection: "+collection);
        } catch (InvalidModelException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage());
        } catch (JsonProcessingException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage());
        } catch (IOException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage());
        }
    }

    private void alterGrants(JsonNode command,String collection,String docId,boolean users,boolean grant ) throws CommandParsingException, UserNotFoundException, DocumentNotFoundException, InvalidCollectionException, InvalidModelException, RoleNotFoundException {
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode node = users?params.get("users"):params.get("roles");
        if (node != null){
            JsonNode read = node.get("read");
            if (read!=null){
                alterGrantsTo(command, read, collection, docId, grant, users, Permissions.ALLOW_READ);
            }
            JsonNode write = node.get("write");
            if (write!=null){
                alterGrantsTo(command,write,collection,docId,grant,users,Permissions.ALLOW_UPDATE);
            }
            JsonNode delete = node.get("delete");
            if (delete!=null){
                alterGrantsTo(command,delete,collection,docId,grant,users,Permissions.ALLOW_DELETE);
            }
            JsonNode all= node.get("all");
            if (all!=null){
                alterGrantsTo(command,all,collection,docId,grant,users,Permissions.ALLOW);
            }

        }
    }

    private void alterGrantsTo(JsonNode command, JsonNode to, String collection, String docId, boolean isGrant,boolean users, Permissions permission) throws CommandParsingException, UserNotFoundException, DocumentNotFoundException, InvalidCollectionException, InvalidModelException, RoleNotFoundException {
        if (!to.isArray()) throw new CommandParsingException(command,"targets of permissions must be an array");
        if (isGrant){
           if (users){
               for (JsonNode u: to){
                   if (!u.isTextual()) throw new CommandParsingException(command,"invalid user name specified: "+u);
                   DocumentService.grantPermissionToUser(collection,docId,permission,u.asText());
               }
           } else {
               for (JsonNode r:to){
                    if (!r.isTextual()) throw new CommandParsingException(command,"invalid role name specified: "+r);
                    DocumentService.grantPermissionToRole(collection,docId,permission,r.asText());
               }
           }
        } else {
            if (users){
                for (JsonNode u: to){
                    if (!u.isTextual()) throw new CommandParsingException(command,"invalid user name specified: "+u);
                    DocumentService.revokePermissionToUser(collection, docId, permission, u.asText());
                }
            } else {
                for (JsonNode r:to){
                    if (!r.isTextual()) throw new CommandParsingException(command,"invalid role name specified: "+r);
                    DocumentService.revokePermissionToRole(collection, docId, permission, r.asText());
                }
            }
        }
    }

    private String getDocumentId(JsonNode command) throws CommandException{
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode id = params.get("id");
        if (id==null||!id.isTextual()){
            throw new CommandParsingException(command,"missing document id");
        }
        String idString = id.asText();
        try{
            UUID.fromString(idString);
        } catch (IllegalArgumentException e){
            throw new CommandParsingException(command,"document id: "+id+" must be a valid uuid");
        }
        return idString;
    }

    @Override
    public String name() {
        return RESOURCE_NAME;
    }
}
