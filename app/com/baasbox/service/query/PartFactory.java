package com.baasbox.service.query;


import static com.baasbox.service.query.PartsLexer.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class PartFactory {

	
	static final String FIELD_STRING_PATTERN ="^\\.([a-zA-Z0-9]+)";
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
			throw new PartValidationException(part, position, "Unrecognized Part Type");
		}
		String error;
		if((error= p.validate()) != null){
			throw new PartValidationException(part, position, error);
		}
		return p;
	}

}
