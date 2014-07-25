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


import static com.baasbox.service.query.PartsLexer.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class PartFactory {

	
	static final String FIELD_STRING_PATTERN ="^\\.([a-zA-Z0-9_-]+)";
	static enum PartPattern {
		
		FIELD(FIELD_STRING_PATTERN+"$"),
		ARRAY(FIELD_STRING_PATTERN+"(\\[([0-9]+)\\])$"),
		RELATION("^\\{([a-zA-Z0-9]+)\\}$");
		
		private String pattern;
		
		PartPattern(String pattern){
			this.pattern = pattern;
		}
		
	}
	
	private static boolean isField(String part){
		return match(part,PartPattern.FIELD.pattern);
	}
	
	private static boolean isArray(String part){
		return match(part,PartPattern.ARRAY.pattern);
	}
	
	private static boolean isRelation(String part){
		return match(part,PartPattern.RELATION.pattern);
	}
	
	private static boolean match(String part,String pattern){
		Pattern p = Pattern.compile(pattern);
		Matcher m =p.matcher(part);
		return m.matches();
	}
	
	public static Part parse(String part,int position) throws PartValidationException {
		Part p = null;
		if(isField(part)){
			p = new Field(part,position);
		}else if(isArray(part)){
			p = new ArrayField(part,position);
		}else if(isRelation(part)){
			//return null;
		}else{
			//return null;
		}
		if(p== null){
			throw new PartValidationException(part, position, "Unrecognized Part Type.HINT: did you put a . before any field?");
		}
		String error;
		if((error= p.validate()) != null){
			throw new PartValidationException(part, position, error);
		}
		return p;
	}

}
