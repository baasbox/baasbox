package com.baasbox.commands;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.baasbox.BBConfiguration;
import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandNotSupportedException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.db.DbHelper;
import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionObject;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.base.JsonCallback;
import com.baasbox.util.BBJson;
import com.baasbox.util.BBJson.ObjectMapperExt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

public class SessionsResource extends BaseRestResource {
	 public static final Resource INSTANCE = new SessionsResource();
	 private static final String RESOURCE_NAME = "sessions";
	 public static final String TOKEN = "token";
	 public static final String PASSWORD = "password";
	 public static final String USERNAME = "username";
	 
	 @Override
	    protected ImmutableMap.Builder<String, ScriptCommand> baseCommands() {
	        return super.baseCommands().put("revokeAllTokensOfAGivenUser", new ScriptCommand() {
	            @Override
	            public JsonNode execute(JsonNode command, JsonCallback unused) throws CommandException {
	                return revokeAllTokensOfAGivenUser(command);
	            }
	        }).put("getCurrent", this::getCurrent);
	    }

	protected JsonNode getCurrent(JsonNode command,JsonCallback unused) throws CommandException  {
		try {
			SessionObject st = SessionTokenProviderFactory.getSessionTokenProvider().getCurrent();
			if (st==null) return NullNode.getInstance();
			ObjectMapperExt mapper = BBJson.mapper();
			String s;
			s = mapper.writeValueAsString(st);
			ObjectNode stToRet = (ObjectNode) mapper.readTree(s);
			stToRet.remove(SessionKeys.PASSWORD.toString());
			return stToRet;
		} catch (JsonProcessingException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		} catch (IOException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		}
	}

	protected JsonNode revokeAllTokensOfAGivenUser(JsonNode command) throws CommandException {
		try {
			super.validateHasParams(command);
			String username=getUsernameFromParam(command);
			ISessionTokenProvider tp = SessionTokenProviderFactory
					.getSessionTokenProvider();
			List<SessionObject> sessions = tp.getSessions(username);
			sessions.forEach(x->tp.removeSession(x.getToken()));
			return IntNode.valueOf(sessions.size());
		} catch (CommandParsingException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw e;
		} 
	}

	/***
	 * Remove a session value by its token
	 */
	@Override
	protected JsonNode delete(JsonNode command) throws CommandException {
		try {
			super.validateHasParams(command);
			String token=getTokenFromParam(command);
			SessionTokenProviderFactory
				.getSessionTokenProvider()
				.removeSession(token);
			return NullNode.getInstance();
		} catch (CommandParsingException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw e;
		} 
	}

	@Override
	protected JsonNode put(JsonNode command) throws CommandException {
		throw new CommandNotSupportedException(command, "It is not possible to update a Session");
	}

	/**
	 * Creates a new session
	 */
	@Override
	protected JsonNode post(JsonNode command) throws CommandException {
		try {
			super.validateHasParams(command);
			String username=getUsernameFromParam(command);
			String password=getPasswordFromParam(command);
			SessionObject st = SessionTokenProviderFactory.getSessionTokenProvider().setSession(DbHelper.getCurrentAppCode(), username, password);
			ObjectMapperExt mapper = BBJson.mapper();
			String s=st.toString();
			ObjectNode stToRet = (ObjectNode) mapper.readTree(s);
			stToRet.remove(SessionKeys.PASSWORD.toString());
			return stToRet;
		} catch (JsonProcessingException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		} catch (CommandParsingException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw e;
		} catch (IOException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		}
		
	}

	private String getPasswordFromParam(JsonNode command) throws CommandParsingException {
		 JsonNode passwordNode = super.getParamField(command, PASSWORD);
	        if (passwordNode == null || !passwordNode.isTextual()){
	            throw new CommandParsingException(command,"invalid \"password\" param: "+(passwordNode==null?"null":passwordNode.toString()));
	        }
	        return passwordNode.asText();
	}

	/**
	 * Returns an Array of session related to a given user
	 */
	@Override
	protected JsonNode list(JsonNode command) throws CommandException {
		try {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionsResource - list method started");
			super.validateHasParams(command);
			String username=getUsernameFromParam(command);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionsResource - list sessions of user {}",username);
			
			List<SessionObject> sessions = 
					SessionTokenProviderFactory
						.getSessionTokenProvider()
						.getSessions(username);
			ObjectMapperExt mapper = BBJson.mapper();
			String s=mapper.writeValueAsString(sessions);
			
			ArrayNode lst = (ArrayNode) mapper.readTree(s);
			lst.forEach((j)->((ObjectNode)j).remove(SessionKeys.PASSWORD.toString()));
			return lst;
		} catch (JsonProcessingException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		} catch (CommandParsingException e){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw e;
		} catch (IOException e) {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug(ExceptionUtils.getMessage(e),e);
			throw new CommandExecutionException(command,ExceptionUtils.getMessage(e),e);
		}
	}

	@Override
	protected JsonNode get(JsonNode command) throws CommandException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		return RESOURCE_NAME;
	}
	
    private String getTokenFromParam(JsonNode command) throws CommandParsingException {
        JsonNode tokenNode = super.getParamField(command, TOKEN);
        if (tokenNode == null || !tokenNode.isTextual()){
            throw new CommandParsingException(command,"invalid \"token\" param: "+(tokenNode==null?"null":tokenNode.toString()));
        }
        return tokenNode.asText();
    }
    
    private String getUsernameFromParam(JsonNode command) throws CommandParsingException {
        JsonNode usernameNode = super.getParamField(command, USERNAME);
        if (usernameNode == null || !usernameNode.isTextual()){
            throw new CommandParsingException(command,"invalid \"username\" param: "+(usernameNode==null?"null":usernameNode.toString()));
        }
        String username=usernameNode.asText();
        if (username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxAdminUsername())
        		||
        	username.equalsIgnoreCase(BBConfiguration.getInstance().getBaasBoxUsername())){
        	throw new CommandParsingException(command,"invalid \"username\" param: "+username+" it is reserved");
        }
        return usernameNode.asText();
    }
	 
}
