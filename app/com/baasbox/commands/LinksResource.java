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
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.ObjectUtils;

import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.commands.exceptions.CommandGenericError;
import com.baasbox.commands.exceptions.CommandNotSupportedException;
import com.baasbox.commands.exceptions.CommandParsingException;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.LinkService;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.baasbox.util.JSONFormats.Formats;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {resource: 'links',
 *  name: <'get'|'list'|'post'|'delete',
 *  params; {
 *      name: <name>,
 *      query: <query>,*
 *      id: <id>*,
 *      sourceId: <sourceid>*,
 *      destId: <sourceid>* 
 *  }}
 *
 *
 *  {resource: 'links',
 *   name: 'get',
 *   params: {
 *       name: <name>,
 *       query: <query>
 *   }}
 *
 *  {resource: 'links',
 *   name: 'post',
 *   params: {
 *       name: <name>,
 *       sourceId: <sourceid>,
 *       destId: <sourceid>
 *   }}
 *
 *
 * Created by Claudio Tesoriero on 04/12/2014
 */
class LinksResource extends BaseRestResource {
    public static final Resource INSTANCE = new LinksResource();

    private static final String RESOURCE_NAME = "links";

    private static final String QUERY = "query";
    private static final String SOURCE_ID = "sourceId";
    private static final String DEST_ID = "destId";
    private static final String LINK_NAME = "label";
    private static final String AUTHOR = "author";

 
    @Override
    protected JsonNode delete(JsonNode command) throws CommandException {
        validateHasParams(command);
        String id = getLinkId(command);
        ODocument link=LinkService.getLink(id);
		if (link==null) return null;
		LinkService.deleteLink(id);
		return null;
    }


    @Override
    protected JsonNode put(JsonNode command) throws CommandException{
    	throw new CommandNotSupportedException(command, "It is not possible to update a link");
    }

    @Override
    protected JsonNode post(JsonNode command) throws CommandException{
        validateHasParams(command);
        String sourceId = getSourceId(command);
        String destId = getDestId(command);
        String edgeName = getLinkName(command);
        
		ODocument toReturn = null;
		try {
			ODocument link = LinkService.createLink(sourceId, destId, edgeName);
			link.save();
			toReturn = (ODocument)link.getRecord().copy();
			//toReturn.detach();
			toReturn.field("out",(ODocument)((ODocument)(link.field("out"))).field("_node"));
			toReturn.field("in",(ODocument)((ODocument)(link.field("in"))).field("_node"));
		}catch (DocumentNotFoundException e){
			throw new CommandGenericError(command,"Source or Destination record was not found. Hint: do you have the read grant on them? Or ids are not valid",e);	
		}
		toReturn.detach();
		String fmt=JSONFormats.prepareDocToJson(toReturn, Formats.LINK);
		toReturn=null;
		JsonNode node;
		ObjectNode n;
		try {
			node = Json.mapper().readTree(fmt);
			n =(ObjectNode)node;
		} catch (IOException e) {
			throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e),e);
		}
		n.remove(TO_REMOVE).remove("@rid");
        return n;
		
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

    
    private String getSourceId(JsonNode command) throws CommandParsingException {
        JsonNode node = command.get(ScriptCommand.PARAMS).get(SOURCE_ID);
        if (node == null||(!node.isTextual())){
            throw new CommandParsingException(command,"missing sourceId");
        }
        return node.asText();
    }
    
    private String getDestId(JsonNode command) throws CommandParsingException {
        JsonNode node = command.get(ScriptCommand.PARAMS).get(DEST_ID);
        if (node == null||(!node.isTextual())){
            throw new CommandParsingException(command,"missing destId");
        }
        return node.asText();
    }
    
    private String getLinkName(JsonNode command) throws CommandParsingException {
        JsonNode node = command.get(ScriptCommand.PARAMS).get(LINK_NAME);
        if (node == null||(!node.isTextual())){
            throw new CommandParsingException(command,"missing destId");
        }
        return node.asText();
    }
    
    @Override
    protected JsonNode list(JsonNode command) throws CommandException {
    	validateHasParams(command);
        QueryParams params = QueryParams.getParamsFromJson(command.get(ScriptCommand.PARAMS).get(QUERY));

		List<ODocument> listOfLinks;
		try {
			listOfLinks = LinkService.getLink(params);
			String s = JSONFormats.prepareDocToJson(listOfLinks, JSONFormats.Formats.LINK);
			ArrayNode lst = (ArrayNode)Json.mapper().readTree(s);
			lst.forEach((j)->((ObjectNode)j).remove(TO_REMOVE).remove("@rid"));
			lst.forEach((j)->(ObjectUtils.firstNonNull((ObjectNode)j.get("in"),Json.mapper().createObjectNode())).remove(TO_REMOVE).remove("@rid"));
			lst.forEach((j)->(ObjectUtils.firstNonNull((ObjectNode)j.get("ot"),Json.mapper().createObjectNode())).remove(TO_REMOVE).remove("@rid"));
			return lst;
		} catch (SqlInjectionException | IOException e) {
            throw new CommandExecutionException(command,"error executing command: "+e.getMessage(),e);
		} 
    }


    @Override
    protected JsonNode get(JsonNode command) throws CommandException{
    	validateHasParams(command);
        String id = getLinkId(command);
        ODocument link=LinkService.getLink(id);
		if (link==null) return null;
		ObjectNode node=null;
		String s = JSONFormats.prepareDocToJson(link, JSONFormats.Formats.LINK);
		try {
			node = (ObjectNode)Json.mapper().readTree(s);
			node.remove(TO_REMOVE).remove("@rid");
			((ObjectNode)node.get("in")).remove(TO_REMOVE).remove("@rid");
			((ObjectNode)node.get("out")).remove(TO_REMOVE).remove("@rid");
		} catch ( IOException e) {
            throw new CommandExecutionException(command,"error executing command: "+ExceptionUtils.getMessage(e),e);
        } 
		return node;
    }


    private String getLinkId(JsonNode command) throws CommandException{
        JsonNode params = command.get(ScriptCommand.PARAMS);
        JsonNode id = params.get("id");
        if (id==null||!id.isTextual()){
            throw new CommandParsingException(command,"missing link id");
        }
        String idString = id.asText();
        try{
            UUID.fromString(idString);
        } catch (IllegalArgumentException e){
            throw new CommandParsingException(command,"link id: "+id+" must be a valid uuid");
        }
        return idString;
    }

    @Override
    public String name() {
        return RESOURCE_NAME;
    }
}
