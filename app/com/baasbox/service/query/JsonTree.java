/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

package com.baasbox.service.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.baasbox.service.query.PartsLexer.Part;

public class JsonTree {
	
	public static JsonNode read(JsonNode json,PartsParser pp){
		JsonNode root = json;
		for (Part p : pp.parts()) {
			if(p instanceof PartsLexer.ArrayField){
				int index = ((PartsLexer.ArrayField)p).arrayIndex;
				root = root.path(p.getName()).path(index);
			}	else if(p instanceof PartsLexer.Field){
				root = root.path(p.getName());
			}
		}
		return root;
	}
	
	public static JsonNode write(JsonNode json,PartsParser pp,JsonNode data) throws MissingNodeException{
		JsonNode root =json;
		for (Part p : pp.parts()) {
			if(p.equals(pp.last())){
				break;
			}
			if(p instanceof PartsLexer.ArrayField){
				int index = ((PartsLexer.ArrayField)p).arrayIndex;
				root = root.path(p.getName()).path(index);
			}	else if(p instanceof PartsLexer.Field){
				root = root.path(p.getName());
			}
		}
		if(root.isMissingNode()){
			throw new MissingNodeException(pp.treeFields());
		}
		Part last = pp.last();
		
		if(last instanceof PartsLexer.ArrayField){
			PartsLexer.ArrayField arr = (PartsLexer.ArrayField)last;
			int index = arr.arrayIndex;
			root = root.path(last.getName());
			ArrayNode arrNode = (ArrayNode)root;
			if(arrNode.size()<=index){
				arrNode.add(data);
			}else{
				arrNode.set(index, data);
			}
			return root;
			
		}else{
			try{
				((ObjectNode)root).put(last.getName(),data);
			}catch(Exception e){
				throw new MissingNodeException(pp.treeFields());
			}
			return root.get(last.getName());
		}
		
	}
	
}
