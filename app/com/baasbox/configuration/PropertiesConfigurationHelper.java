package com.baasbox.configuration;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;

import com.baasbox.exception.ConfigurationException;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;



public class PropertiesConfigurationHelper {

	/***
	 * This are the [sections] inside the configuration.conf file.
	 * Each of them maps an Enum
	 */
	public static final ImmutableMap<String,Class> configurationSections = ImmutableMap.of(
														 "PasswordRecovery",(Class)PasswordRecovery.class
														,"Application",(Class)Application.class
														,"Images",(Class)ImagesConfiguration.class
	);
	
	

	
	/***
	 *
	 * Returns a json representation of the Enumerator
	 * The Enumerator must implements the IProperties interface
	 * @param en 	the Enumerator to serialize. It must implements the IProperties interface
	 * @return 		the representation of the Enumerator 
	 */
	@SuppressWarnings("unchecked")
	public static String dumpConfigurationAsJson(String section) {
		Class en = configurationSections.get(section);
		try {
			JsonFactory jfactory = new JsonFactory();
			StringWriter sw = new StringWriter();
			String enumDescription = "";			
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			
			Method getEnumDescription= en.getMethod("getEnumDescription");
			if (getEnumDescription!=null  && getEnumDescription.getReturnType()==String.class && Modifier.isStatic(getEnumDescription.getModifiers()))
					enumDescription=(String) getEnumDescription.invoke(null);
			gen.writeStartObject();																						//{
			gen.writeStringField("section", section);													//	 "configuration":"EnumName"
			gen.writeStringField("description", enumDescription);														//	,"description": "EnumDescription"
			gen.writeFieldName("sub sections");																				//  ,"sections":
			gen.writeStartObject();																						//		{
			String lastSection = "";
			EnumSet values = EnumSet.allOf( en );
			for (Object v : values) {
				  String key=(String) (en.getMethod("getKey")).invoke(v);
				  String valueAsString=(String) (en.getMethod("getValueAsString")).invoke(v);
				  String valueDescription=(String) (en.getMethod("getValueDescription")).invoke(v);
				  Class type = (Class) en.getMethod("getType").invoke(v);
			      String subsection = key.substring(0, key.indexOf('.'));
			      if (!lastSection.equals(subsection)) {
			    	if (gen.getOutputContext().inArray()) gen.writeEndArray();
			        gen.writeFieldName(subsection);																		//			"sectionName":
			        gen.writeStartArray();																				//				[
			        lastSection = subsection;
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
		ImmutableCollection<String> keys = configurationSections.keySet();  
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jfactory = mapper.getJsonFactory();
		StringWriter sw = new StringWriter();	
		try{
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			gen.writeStartArray();	
			for (String v: keys){
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
		ImmutableCollection<String> keys = configurationSections.keySet(); 
		StringBuilder sb = new StringBuilder();
		for (String v: keys){
			sb.append(dumpConfiguration(v));
			sb.append("\n");
		}
		return sb.toString();
	}//dumpConfiguration()
	

	
	public static String dumpConfiguration(String section) {
		Class en = configurationSections.get(section);
		try {
			StringBuilder sb = new StringBuilder();
			String enumDescription = "";			
			
			Method getEnumDescription= en.getMethod("getEnumDescription");
			if (getEnumDescription!=null && getEnumDescription.getReturnType()==String.class && Modifier.isStatic(getEnumDescription.getModifiers()))
					enumDescription=(String) getEnumDescription.invoke(null);
			
		    sb.append(enumDescription);
		    sb.append("\n");
		    sb.append(section.toUpperCase());
		    sb.append("\n");
		    
		    String lastSection = "";
		    EnumSet values = EnumSet.allOf( en );
	    	for (Object  v : values) {
				String key=(String) ((Method)v.getClass().getMethod("getKey")).invoke(v);
				Object value=((Method)en.getMethod("getValue")).invoke(v);
				String subsection = key.substring(0, key.indexOf('.'));
	
			      if (!lastSection.equals(subsection)) {
			        sb.append("  - ");
			        sb.append(subsection.toUpperCase());
			        sb.append("\n");
			        lastSection = subsection;
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
	 * @return the enumerator value
	 * @throws ConfigurationException 
	 * @throws Exception if the en Class is not an Enumerator that implements the IProperties interface
	 */
	public static Object findByKey(Class en,String iKey) throws ConfigurationException {
		EnumSet values = EnumSet.allOf( en );
	    for (Object v : values) {
	        try {
				if ( ((String)en.getMethod("getKey").invoke(v)).equalsIgnoreCase(iKey)  )
				  return v;
			} catch (Exception e) {
				throw new ConfigurationException ("Is it " + en.getCanonicalName() + " an Enum that implements the IProperties interface?",e );
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
	 * @throws ConfigurationException 
	 * @throws Exception
	 */
	public static void setByKey(Class en,String iKey,Object value) throws ConfigurationException  {
		Object enumValue = findByKey(en,iKey);
		try {
			en.getMethod("setValue",Object.class).invoke(enumValue,value);
		} catch (Exception e) {
			throw new ConfigurationException ("Invalid key -" +iKey+ "- or value -" +value  ,e );
		}
	}	//setByKey
	
}//PropertiesConfigurationHelper
