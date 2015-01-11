package com.baasbox.commands;

import java.util.Map;
import java.util.UUID;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.dao.GenericDao;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.base.JsonCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.id.ORID;

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
            ImmutableMap.<String,ScriptCommand>builder()
                        .put("switchUser", DBResource::switchUser)
                        .put("isAdmin",DBResource::isConnectedAsAdmin)
                        .put("beginTransaction",DBResource::beginTransaction)
                        .put("isInTransaction",DBResource::isInTransaction)
                        .put("commitTransaction",DBResource::commitTransaction)
                        .put("rollbackTransaction",DBResource::rollbackTransaction)
                        .put("isAnId",DBResource::isAnId)
                        .build();

//            ImmutableMap.of("switchUser", DBResource::switchUser,
//                            "transact", DBResource::runInTransaction,
//                            "isAdmin",DBResource::isConnectedAsAdmin,
//                            "isInTransaction",DBResource::isInTransaction,
//                            "abortTransaction",DBResource::abortTransation);



//    private static final ThreadLocal<Boolean> JS_TRANSACTION_RUNNING = new ThreadLocal<Boolean>(){
//        @Override
//        protected Boolean initialValue() {
//            return false;
//        }
//    };

    private static JsonNode beginTransaction(JsonNode c,JsonCallback callback) throws CommandException{
        DbHelper.requestTransaction();
        return NullNode.getInstance();
    }

    private static JsonNode rollbackTransaction(JsonNode c,JsonCallback callback) throws CommandException{
        DbHelper.rollbackTransaction();
        return NullNode.getInstance();
    }

    private static JsonNode commitTransaction(JsonNode command,JsonCallback callback) throws CommandException {
        DbHelper.commitTransaction();
        return NullNode.getInstance();
    }

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
        return BooleanNode.valueOf(DbHelper.isConnectedAsAdmin(false));
    }

//    private static JsonNode runInTransaction(JsonNode command,JsonCallback callback) throws CommandException{
//        boolean commit = true;
//        try {
//
//            DbHelper.requestTransaction();
//            JS_TRANSACTION_RUNNING.set(true);
//            JsonNode res = callback.call(NullNode.getInstance());
//            JS_TRANSACTION_RUNNING.set(false);
//            return res;
//        }catch (AbortTransaction t){
//            commit = false;
//            return NullNode.getInstance();
//        }catch (Exception e){
//            commit = false;
//            throw new CommandExecutionException(command,e.getMessage(),e);
//        } finally {
//            if (commit){
//                DbHelper.commitTransaction();
//            } else {
//                DbHelper.rollbackTransaction();
//            }
//        }
//    }

    private static JsonNode isInTransaction(JsonNode command,JsonCallback callback) throws CommandException {
        return BooleanNode.valueOf(DbHelper.isInTransaction());
    }

    private static JsonNode isAnId(JsonNode command,JsonCallback callback) throws CommandException {
    	validateHasParams(command);
    	String id = null;
    	try {
    		id=getLinkId(command);
    	}catch (CommandParsingException e){
    		return BooleanNode.valueOf(false);
    	}
    	ORID ret=GenericDao.getInstance().getRidNodeByUUID(id);
        return BooleanNode.valueOf(!(ret==null));
    }
    
    private static void validateHasParams(JsonNode command) throws CommandParsingException{
        if (!command.has(ScriptCommand.PARAMS)) {
            throw new CommandParsingException(command,"missing parameters");
        }
    }

    private static String getLinkId(JsonNode command) throws CommandException{
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode id = params.get("id");
        if (id==null||!id.isTextual()){
            throw new CommandParsingException(command,"missing id");
        }
        String idString = id.asText();
        try{
            UUID.fromString(idString);
        } catch (IllegalArgumentException e){
            throw new CommandParsingException(command,"id: "+id+" must be a valid uuid");
        }
        return idString;
    }

}
