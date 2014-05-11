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
