package com.baasbox.commands;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Created by eto on 22/09/14.
 */
class DBResource extends Resource {

    public static final Resource INSTANCE = new DBResource();

    @Override
    public String name() {
        return "db";
    }

    @Override
    public Map<String, ScriptCommand> commands() {
        return COMMANDS;
    }

    public static final Map<String,ScriptCommand> COMMANDS =
            ImmutableMap.of("switchUser", DBResource::switchUser,
                            "transact", DBResource::runInTransaction,
                            "isAdmin",DBResource::isConnectedAsAdmin,
                            "isInTransaction",DBResource::isInTransaction);



    private static JsonNode switchUser(JsonNode command,JsonCallback callback) throws CommandException {
        if (DbHelper.isInTransaction()) throw new CommandExecutionException(command,"Cannot switch to admin during a transaction");
        try {
            DbHelper.reconnectAsAdmin();
            return callback.call(NullNode.getInstance());
        } finally {
            DbHelper.reconnectAsAuthenticatedUser();
        }
    }

    private static JsonNode isConnectedAsAdmin(JsonNode command,JsonCallback callback) throws CommandException {
        return BooleanNode.valueOf(DbHelper.isConnectedAsAdmin(true));
    }

    private static JsonNode runInTransaction(JsonNode command,JsonCallback callback) throws CommandException{
        boolean commit = false;
        try {

            DbHelper.requestTransaction();

            JsonNode res = callback.call(NullNode.getInstance());
            commit = res ==null||res.isNull()||(res.isBoolean()&&res.asBoolean());

            return NullNode.getInstance();
        }catch (Exception e){
            commit = false;
            throw new CommandExecutionException(command,e.getMessage(),e);
        } finally {
            if (commit){
                DbHelper.commitTransaction();
            } else {
                DbHelper.rollbackTransaction();
            }
        }
    }

    private static JsonNode isInTransaction(JsonNode command,JsonCallback callback) throws CommandException {
        return BooleanNode.valueOf(DbHelper.isInTransaction());
    }



}
