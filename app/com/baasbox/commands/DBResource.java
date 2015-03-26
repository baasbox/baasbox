package com.baasbox.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.dao.GenericDao;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.SwitchUserContextException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.util.JSONFormats;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.exception.OQueryParsingException;
import com.orientechnologies.orient.core.exception.OSecurityAccessException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
                        .put("select",DBResource::select)
                        .put("exec",DBResource::exec)
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

    private static JsonNode select(JsonNode c,JsonCallback callback) throws CommandException{
    	JsonNode jParams = c.get(ScriptCommand.PARAMS);
    	String statement = jParams.get("query").asText();
    	JsonNode depthNode = jParams.get("depth");
    	String depth="";
    	if (depthNode!=null && depthNode.isTextual()) depth=",depth:"+depthNode.asText();
    	BaasBoxLogger.debug("Executing query from a plugin: " + statement);
    	BaasBoxLogger.debug("...depth: " + depth);
    	
        ArrayNode qryParams = (ArrayNode) jParams.get("array_of_params");
        
        ArrayList params=new ArrayList();
        if (qryParams!=null) qryParams.forEach(j->{
            if (j==null) params.add(null);
            else params.add(j.asText());
        });
        ArrayNode lst;
		try {
	        List listToReturn = (List) DbHelper.genericSQLStatementExecute("select " + statement, params.toArray());
	        String s = JSONFormats.prepareResponseToJson(listToReturn, JSONFormats.Formats.GENERIC+depth,true);
	        BaasBoxLogger.debug("Query result: ");
	        BaasBoxLogger.debug(s);
			lst = (ArrayNode)Json.mapper().readTree(s);
		} catch (IOException e) {
			 throw new CommandExecutionException(c,"error executing command: "+e.getMessage(),e);
		} catch(OQueryParsingException e){
			throw new CommandExecutionException(c,"Error parsing query: "+e.getMessage(),e);
		}
        return lst;
    }
   
    
    private static JsonNode exec(JsonNode c,JsonCallback callback) throws CommandException{
    	JsonNode jParams = c.get(ScriptCommand.PARAMS);
    	String statement = jParams.get("statement").asText();
    	BaasBoxLogger.debug("Executing statement from a plugin: " + statement);
    	
        ArrayNode qryParams = (ArrayNode) jParams.get("array_of_params");
        
        ArrayList params=new ArrayList();
        if (qryParams!=null) qryParams.forEach(j->{
            if (j==null) params.add(null);
            else params.add(j.asText());
        });
        JsonNode lst;
		try {
	        Object listToReturn = (Object) DbHelper.genericSQLStatementExecute(statement, params.toArray());
	        String s = "";
	        if (listToReturn instanceof List ) s=JSONFormats.prepareResponseToJson((List)listToReturn, JSONFormats.Formats.GENERIC,true);
	        else if (listToReturn instanceof ODocument) s=JSONFormats.prepareResponseToJson((ODocument)listToReturn, JSONFormats.Formats.GENERIC,true);
	        else s=listToReturn.toString();
	        BaasBoxLogger.debug("Statement result: ");
	        BaasBoxLogger.debug(s);
			lst = Json.mapper().readTree(s);
		} catch (IOException e) {
			 throw new CommandExecutionException(c,"error executing command: "+e.getMessage(),e);
		} catch(OQueryParsingException e){
			throw new CommandExecutionException(c,"Error parsing statement: "+e.getMessage(),e);
		}
        return lst;
    }
    
    
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
        try {
            DbHelper.reconnectAsAdmin();
            return callback.call(NullNode.getInstance());
        }catch (SwitchUserContextException e){
        	throw new CommandExecutionException(command,"Cannot switch to admin! Did you leave an open transaction?");
    	}finally {
    		try{
    			DbHelper.reconnectAsAuthenticatedUser();
    		}catch(OSecurityAccessException e){
    			//if the script has changed username or password of the actual user, her credentials are not valid anymore and the db connection is lost
    			BaasBoxLogger.warn("Database connection is not available inside a Plugin Script");
    			//swallow
    		}
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
