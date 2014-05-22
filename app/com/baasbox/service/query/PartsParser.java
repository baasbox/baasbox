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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;

import com.baasbox.service.query.PartsLexer.ArrayField;
import com.baasbox.service.query.PartsLexer.Part;

public class PartsParser {
	
	final String OBJ_PATRN = "\"%s\": %s";
	final String LST_PATRN = "\"%s\": {%s}";
	
	List<Part> parts;
	
	public PartsParser(List<Part> queryParts) {
		if(queryParts==null || queryParts.isEmpty()){
			throw new RuntimeException("You can't initialize the parser with an empty parts array");
		}
		this.parts = queryParts;
	}
	
	public Part first(){
		return this.parts.get(0);
	}
	
	public Part lastParent(){
		if(this.parts.size()>1){
			return this.parts.get(this.parts.size()-2);
		}else{
			return first();
		}
	}
	
	
	public JsonNode json(JsonNode bodyJson) {
		Collections.reverse(this.parts);
		ObjectNode on = Json.newObject();
		Part last = null;
		for(Part p: parts){
			if(last==null){
				on.put(p.getName(), bodyJson);
				last = p;
			}else{
				ObjectNode cont = Json.newObject();
				cont.put(p.getName(),on);
				on = cont;
			}
			
		}
		
		return on;
		
	}
	
	public String treeFields() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.parts.size(); i++) {
			Part p = this.parts.get(i);
			if(i>0){
				sb.append(".");
			}
			sb.append(p.getName());
			
		}
		return sb.toString();
		
	}
	public String fullTreeFields() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < this.parts.size(); i++) {
			Part p = this.parts.get(i);
			if(i>0){
				sb.append(".");
			}
			sb.append(p.getName());
			if(p instanceof ArrayField){
				ArrayField af  = (ArrayField)p;
				sb.append("[").append(af.arrayIndex).append("]");
			}
			
		}
		return sb.toString();
		
	}

	public boolean isMultiField() {
		return this.parts.size() >1;
	}

	public Part last() {
		 return this.parts.get(this.parts.size()-1);
	}

	public List<Part> parts() {
		return this.parts;
	}

	public boolean isArray() {
		return last() instanceof ArrayField;
	}

	
	
	
	
	

}
