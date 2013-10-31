package com.baasbox.service.query;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

public class PartsLexer {
	
	public Part parse(String part,int position){
		return PartFactory.parse(part,position);
	}
	
	public static abstract class Part {
		
		String source;
		int  position;
		
		public abstract String getName();
		
		public Part(String part){
			this.source = part;
			this.position = 1;
		}
		public Part(String part,int position){
			this.source = part;
			this.position = position;
		}
		
		protected static String getGroupName(String input,String groupName,Pattern pattern){
			Matcher m = pattern.matcher(input);
			if(m.matches()){
				return m.group(groupName);
			}
			return null;
		}
		
		
		
		public abstract String validate() throws PartValidationException;
	} 
	
	public static class PartValidationException extends RuntimeException {
		
		public PartValidationException(String source,int position,String reason){
			super(String.format("Part %s at position %d is not valid:%s",source,position,reason));
		}

	} 
	
	public static class Field extends Part{
		static final Pattern p = Pattern.compile("^(?<name>(\\w)+)(\\[(?<arrayIndex>[0-9]+)\\])?$"); 
		public String fieldName;
		protected Field(String source,int position){
			super(source,position);
			this.fieldName = getFieldName();
		}
		
		@Override
		public String getName() {
			return this.fieldName;
		}
		
		private String getFieldName(){
			return getGroupName(this.source.substring(1), "name",p);
		}
		
		@Override
		public String validate() throws PartValidationException {
			boolean result = this.source.startsWith(".");
			String error = null;
			if(!result){
				 error = "Field should starts with a .";
			}
			if(this.fieldName==null){
				error = "Field name is not compliant.It could only contain lowercase/uppercase letters and/or digits";
			}
			
			return error;
		}
		
		
	}
	
	public static class ArrayField extends Field{
		
		private int getArrayIndex(){
			int result = -1;
			String index;
			if((index = getGroupName(this.source.substring(1), "arrayIndex", p))!=null){
				result = Integer.parseInt(index);
			}
			return result;
		}
		public int arrayIndex;
		protected ArrayField(String source,int position){
			super(source,position);
			arrayIndex = getArrayIndex();
		}
		
		@Override
		public String validate() throws PartValidationException {
			String error = super.validate();
			if(error==null){
				int index = getArrayIndex();
				if(index<0){
					error = "The index provided for the array should be >= 0";
				}
			}
			return error;
		}
		
		
	}
}
