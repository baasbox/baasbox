package com.baasbox.configuration;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;



public class PropertiesConfigurationHelper {

	public static final ImmutableMap<String,Class> configurationSections = ImmutableMap.of(
														 "PasswordRecovery",(Class)PasswordRecovery.class
														,"Application",(Class)Application.class
	);
	
	
	/***
	 * Returns a json representation of the Enumerator
	 * The Enumerator must implements the IProperties interface
	 * @param en 	the Enumerator to serialize. It must implements the IProperties interface
	 * @return 		the representation of the Enumerator 
	 */
	@SuppressWarnings("unchecked")
	public static String dumpConfigurationAsJson(Class en) {
		
		try {
			JsonFactory jfactory = new JsonFactory();
			StringWriter sw = new StringWriter();
			String enumDescription = "";			
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			
			Method getEnumDescription= en.getMethod("getEnumDescription");
			if (getEnumDescription!=null  && getEnumDescription.getReturnType()==String.class && Modifier.isStatic(getEnumDescription.getModifiers()))
					enumDescription=(String) getEnumDescription.invoke(null);
			gen.writeStartObject();																						//{
			gen.writeStringField("configuration", en.getSimpleName());													//	 "configuration":"EnumName"
			gen.writeStringField("description", enumDescription);														//	,"description": "EnumDescription"
			gen.writeFieldName("sections");																				//  ,"sections":
			gen.writeStartObject();																						//		{
			String lastSection = "";
			EnumSet values = EnumSet.allOf( en );
			for (Object v : values) {
				  String key=(String) (en.getMethod("getKey")).invoke(v);
				  String valueAsString=(String) (en.getMethod("getValueAsString")).invoke(v);
				  String valueDescription=(String) (en.getMethod("getValueDescription")).invoke(v);
				  Class type = (Class) en.getMethod("getType").invoke(v);
			      String section = key.substring(0, key.indexOf('.'));
			      if (!lastSection.equals(section)) {
			    	if (gen.getOutputContext().inArray()) gen.writeEndArray();
			        gen.writeFieldName(section);																		//			"sectionName":
			        gen.writeStartArray();																				//				[
			        lastSection = section;
			      }	
			      gen.writeStartObject();																				//					{
			      gen.writeStringField(key,valueAsString);															//							"key": "value"	
			      gen.writeStringField("description", valueDescription);												//						,"description":"description"
			      gen.writeStringField("type",type.getSimpleName());													//						,"type":"type"
			      gen.writeEndObject();																					//					}
			}
			if (gen.getOutputContext().inArray()) gen.writeEndArray();													//				]
			gen.writeEndObject();																						//		}
			gen.writeEndObject();																					//}
			gen.close();
			return sw.toString();
		} catch (Exception e) {
			Logger.error("Cannot generate a json for "+ en.getSimpleName()+" Enum. Is it an Enum that implements the IProperties interface?",e);
		}
		return "{}";
	}//dumpConfigurationAsJson(en)
	
	public static String dumpConfigurationAsJson(){
		ImmutableCollection<Class> values = configurationSections.values(); 
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jfactory = mapper.getJsonFactory();
		StringWriter sw = new StringWriter();	
		try{
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			gen.writeStartArray();	
			for (Class v: values){
				String st = dumpConfigurationAsJson(v);
				JsonParser jp = jfactory.createJsonParser(st);
				gen.writeTree(jp.readValueAsTree());
			}
			gen.writeEndArray();
			gen.close();
			return sw.toString();
		}catch (Exception e) {
			Logger.error("Cannot generate a json for the configuration",e);
		}
		return "[]";
	}//dumpConfigurationAsJson()	
	
	public static String dumpConfiguration(){
		ImmutableCollection<Class> values = configurationSections.values(); 
		StringBuilder sb = new StringBuilder();
		for (Class v: values){
			sb.append(dumpConfiguration(v));
			sb.append("\n");
		}
		return sb.toString();
	}//dumpConfiguration()
	
	public static String dumpConfiguration(Class en) {

		try {
			StringBuilder sb = new StringBuilder();
			String enumDescription = "";			
			
			Method getEnumDescription= en.getMethod("getEnumDescription");
			if (getEnumDescription!=null && getEnumDescription.getReturnType()==String.class && Modifier.isStatic(getEnumDescription.getModifiers()))
					enumDescription=(String) getEnumDescription.invoke(null);
			
		    sb.append(enumDescription);
		    sb.append("\n");
	
		    String lastSection = "";
		    EnumSet values = EnumSet.allOf( en );
	    	for (Object  v : values) {
				String key=(String) ((Method)v.getClass().getMethod("getKey")).invoke(v);
				Object value=((Method)en.getMethod("getValue")).invoke(v);
				String section = key.substring(0, key.indexOf('.'));
	
			      if (!lastSection.equals(section)) {
			        sb.append("  - ");
			        sb.append(section.toUpperCase());
			        sb.append("\n");
			        lastSection = section;
			      }
			      sb.append("      + ");
			      sb.append(key);
			      sb.append(" = ");
			      sb.append(value);
			      sb.append("\n");
		    }
		    return sb.toString();
		} catch (Exception e) {
			Logger.error("Cannot generate a json for "+ en.getSimpleName()+" Enum. Is it an Enum that implements the IProperties interface?",e);
		}
		return "";
	}//dumpConfiguration
	
	/***
	 * Returns an Enumerator value by its key
	 * The Enumerator must implement the IProperties interface
	 * @param en
	 * @param iKey
	 * @return the enumeratov value
	 * @throws Exception if the en Class is not an Enumerator that implements the IProperties interface
	 */
	public static Object findByKey(Class en,String iKey) throws Exception {
		EnumSet values = EnumSet.allOf( en );
	    for (Object v : values) {
	        try {
				if ( ((String)en.getMethod("getKey").invoke(v)).equalsIgnoreCase(iKey)  )
				  return v;
			} catch (Exception e) {
				throw new Exception ("Is it " + en.getCanonicalName() + " an Enum that implements the IProperties interface?",e );
			}
	      }
		return null;
	}	//findByKey
	
	/***
	 * Set an Enumerator value.
	 * The Enumerator class must implement the IProperties interface
	 * @param en The Enumerator class
	 * @param iKey
	 * @param value
	 * @throws Exception
	 */
	public static void setByKey(Class en,String iKey,Object value) throws Exception {
		Object enumValue = findByKey(en,iKey);
		en.getMethod("setValue",Object.class).invoke(enumValue,value);
	}	//setByKey
	
}//PropertiesConfigurationHelper
