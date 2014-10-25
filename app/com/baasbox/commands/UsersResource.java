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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandNotImplementedException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.AlreadyFriendsException;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.user.FriendShipService;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;
import scala.util.parsing.combinator.testing.Str;

/**
 * Created by Andrea Tortorella on 02/07/14.
 */
class UsersResource extends BaseRestResource {
    public static final Resource INSTANCE = new UsersResource();

    @Override
    protected ImmutableMap.Builder<String, ScriptCommand> baseCommands() {
        return super.baseCommands().put("suspend", new ScriptCommand() {
            @Override
            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                return suspend(command);
            }
        }).put("reactivate", new ScriptCommand() {
            @Override
            public JsonNode execute(JsonNode command, JsonCallback callback) throws CommandException {
                return reactivate(command);
            }
        }).put("follow", this::friendshipUpdate)
          .put("followers",getFriends(false))
          .put("following",getFriends(true));
    }

    private ScriptCommand getFriends(boolean following){
        return (command,unused) ->{
            JsonNode params = command.get(ScriptCommand.PARAMS);
            if (params == null) throw new CommandParsingException(command,"missing required parameters");
            JsonNode user = params.path("user");
            JsonNode query = params.get("query");
            if (!user.isTextual()) throw new CommandParsingException(command,"missing required paramter user as string");
            if (query!=null && !query.isObject()) throw new CommandParsingException(command,"query must be a json object");
            QueryParams qparams = QueryParams.getParamsFromJson(query);
            try {

                List<ODocument> res = following ?
                        FriendShipService.getFollowing(user.asText(), qparams) :
                        FriendShipService.getFriendsOf(user.asText(), qparams);
                String s = JSONFormats.prepareDocToJson(res, JSONFormats.Formats.USER);

                return Json.mapper().readTreeOrMissing(s);
            } catch (SqlInjectionException e){
                throw new CommandExecutionException(command,e.getMessage(),e);
            }
        };
    }

//    private JsonNode followers(JsonNode command, JsonCallback unused) throws CommandException{
//        try {
//            List<ODocument> friendsOf = FriendShipService.getFriendsOf(user.asText(), QueryParams.getParamsFromJson(query));
//            String s = JSONFormats.prepareResponseToJson(friendsOf, JSONFormats.Formats.USER);
//            return Json.mapper().readTreeOrMissing(s);
//        } catch (SqlInjectionException | IOException e) {
//            throw new CommandExecutionException(command,e.getMessage(),e);
//        }
//    }

    protected JsonNode friendshipUpdate(JsonNode command,JsonCallback unused) throws CommandException {
        JsonNode params = command.get(ScriptCommand.PARAMS);
        if (params == null) throw new CommandParsingException(command,"missing required parameters");
        JsonNode from = params.get("from");
        JsonNode to = params.get("to");
        JsonNode remove = params.get("remove");
        if (from==null||!from.isTextual()||
            to == null||!to.isTextual())
            throw new CommandParsingException(command,"missing required user");
        boolean unfollow;
        if (remove == null){
            unfollow = false;
        } else if (remove.isBoolean()){
            unfollow =remove.asBoolean();
        } else {
            throw new CommandParsingException(command,"wrong parameter remove");
        }
        if (unfollow){
            return  doUnfollow(command, from.asText(), to.asText());
        } else {
            return doFollow(command, from.asText(), to.asText());
        }
    }

    private JsonNode doUnfollow(JsonNode command,String from,String to) throws CommandExecutionException{
        try {
            return BooleanNode.valueOf(FriendShipService.unfollow(from, to));
        } catch (Exception e) {
            throw new CommandExecutionException(command,e.getMessage(),e);
        }
    }

    private JsonNode doFollow(JsonNode command,String from,String to) throws CommandExecutionException{
        try {
            ODocument followed = FriendShipService.follow(from, to);
            String s = JSONFormats.prepareDocToJson(followed, JSONFormats.Formats.USER);
            return Json.mapper().readTree(s);
        } catch (UserNotFoundException e) {
            throw new CommandExecutionException(command,e.getMessage(),e);
        } catch (AlreadyFriendsException e) {
            return NullNode.getInstance();
        } catch (SqlInjectionException e) {
            throw new CommandExecutionException(command,e.getMessage(),e);
        } catch (Exception e) {
            throw new CommandExecutionException(command,e.getMessage(),e);
        }
    }




    protected JsonNode suspend(JsonNode command) throws CommandException {
        String username = getUsername(command);

        try {
            boolean inTransaction = DbHelper.isInTransaction();
            if (inTransaction){
                return BooleanNode.getFalse();
            }
            UserService.disableUser(username);
        } catch (UserNotFoundException e) {
            throw new CommandExecutionException(command,"User "+username+" does not exists");
        } catch (OpenTransactionException e){
            return BooleanNode.getFalse();
            //throw new CommandExecutionException(command,"Transaction still open during suspend");
        }
        return BooleanNode.getTrue();
    }

    private String getUsername(JsonNode command) throws CommandException {
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode id = params.get("username");
        if (id==null||!id.isTextual()){
            throw new CommandParsingException(command,"missing user username");
        }

        String username = id.asText();
        boolean internalUsername = UserService.isInternalUsername(username);
        if (internalUsername){
            throw new CommandExecutionException(command,"invalid user: "+username);
        }
        return username;
    }

    protected JsonNode reactivate(JsonNode command) throws CommandException {
        String username = getUsername(command);
        try {
            if (DbHelper.isInTransaction()) return BooleanNode.getFalse();
            UserService.enableUser(username);

        } catch (UserNotFoundException e) {
            throw new CommandExecutionException(command,"user "+username+ " does not exists");
        } catch (OpenTransactionException e){
            return BooleanNode.getFalse();
            //throw new CommandExecutionException(command,"transaction still open while altering user status");
        }
        return BooleanNode.getTrue();
    }
	
    @Override
    protected JsonNode delete(JsonNode command) throws CommandException {
        throw new CommandNotImplementedException(command,"not implemented");
    }

    @Override
    protected JsonNode put(JsonNode command) throws CommandException {
        String username = getUsername(command);
        JsonNode params = command.get(ScriptCommand.PARAMS);
        String role = params.get("role")==null?params.get("role").asText():null;
        JsonNode userVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
        JsonNode friendsVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
        JsonNode registeredVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
        JsonNode anonymousVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
        try {
            ODocument doc = UserService.updateProfile(username, role, anonymousVisible, userVisible, friendsVisible, registeredVisible);
            String s = JSONFormats.prepareDocToJson(doc, JSONFormats.Formats.USER);
            return Json.mapper().readTree(s);
        } catch (Exception e) {
            throw new CommandExecutionException(command,"Error updating user: "+e.getMessage());
        }
    }

    @Override
    protected JsonNode post(JsonNode command) throws CommandException {
        try {
            JsonNode params = command.get(ScriptCommand.PARAMS);
            if (params==null||!params.isObject()) throw new CommandParsingException(command,"missing parameters");
            String username = getUsername(command);
            JsonNode password = params.get("password");
            if (password==null||!password.isTextual()) throw new CommandParsingException(command,"missing required password");
            JsonNode roleNode = params.get("role");
            String role;
            if (roleNode == null){
                role = DefaultRoles.REGISTERED_USER.getORole().getName();
            } else if (roleNode.isTextual()){
                role = roleNode.asText();
            } else {
                throw new CommandParsingException(command,"role parameter is not valid");
            }
            if (!RoleService.exists(role)){
                throw new CommandExecutionException(command,"required role does not exists: "+role);
            }
            JsonNode userVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
            JsonNode friendsVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
            JsonNode registeredVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
            JsonNode anonymousVisible = params.get(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);

            ODocument user = UserService.signUp(username, password.asText(),
                                                new Date(), role,
                                                anonymousVisible,userVisible,friendsVisible, registeredVisible, false);
            String userNode = JSONFormats.prepareDocToJson(user, JSONFormats.Formats.USER);
            return Json.mapper().readTree(userNode);
        } catch (InvalidJsonException | IOException e) {
            throw new CommandExecutionException(command,"invalid json",e);
        } catch (UserAlreadyExistsException e) {
            return NullNode.getInstance();
        }
    }

    @Override
    protected JsonNode list(JsonNode command) throws CommandException {
        JsonNode paramsNode = command.get(ScriptCommand.PARAMS);
        QueryParams qp = QueryParams.getParamsFromJson(paramsNode);
        try {
            List<ODocument> users = UserService.getUsers(qp, true);
            String response = JSONFormats.prepareDocToJson(users, JSONFormats.Formats.USER);
            return Json.mapper().readTree(response);
        } catch (SqlInjectionException e) {
            throw new CommandExecutionException(command, "error executing command: " + e.getMessage());
        } catch (IOException e) {
            throw new CommandExecutionException(command, "error parsing response: " + e.getMessage());
        }
    }

    @Override
    protected JsonNode get(JsonNode command) throws CommandException {
        String user = getUsername(command);
        try {
            if (UserService.isInternalUsername(user)) return NullNode.getInstance();
            ODocument doc = UserService.getUserProfilebyUsername(user);
            if (doc == null){
                return NullNode.getInstance();
            }
            String resp = JSONFormats.prepareDocToJson(doc,JSONFormats.Formats.USER);
            return Json.mapper().readTree(resp);
        } catch (SqlInjectionException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage());
        } catch (IOException e) {
            throw new CommandExecutionException(command,"error parsing response: "+e.getMessage());
        }
    }

    @Override
    public String name() {
        return "users";
    }
}
