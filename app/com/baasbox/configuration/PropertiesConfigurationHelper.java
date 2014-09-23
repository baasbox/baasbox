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

package com.baasbox.configuration;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.commons.lang3.StringUtils;

import play.Logger;

import com.baasbox.exception.ConfigurationException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.PushSwitchException;
import com.baasbox.service.push.providers.PushInvalidApiKeyException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;



public class PropertiesConfigurationHelper {

	/***
	 * This are the [sections] inside the configuration.conf file.
	 * Each of them maps an Enum
	 */
	public static final ImmutableMap<String,Class> CONFIGURATION_SECTIONS = ImmutableMap.of(
														 "PasswordRecovery",(Class)PasswordRecovery.class
														,"Application",(Class)Application.class
														,"Push",(Class)Push.class
														,"Images",(Class)ImagesConfiguration.class
														,"Social",(Class)SocialLoginConfiguration.class
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
		Class en = CONFIGURATION_SECTIONS.get(section);
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
				  boolean isVisible=(Boolean)(en.getMethod("isVisible")).invoke(v);
				  String valueAsString;
				  if (isVisible) valueAsString=(String) (en.getMethod("getValueAsString")).invoke(v);
				  else valueAsString = "--HIDDEN--";
				  boolean isEditable=(Boolean)(en.getMethod("isEditable")).invoke(v);
				  String valueDescription=(String) (en.getMethod("getValueDescription")).invoke(v);
				  Class type = (Class) en.getMethod("getType").invoke(v);
			      String subsection = key.substring(0, key.indexOf('.'));
			      if (!lastSection.equals(subsection)) {
			    	if (gen.getOutputContext().inArray()) gen.writeEndArray();
			        gen.writeFieldName(subsection);																		//			"sectionName":
			        gen.writeStartArray();																				//				[
			        lastSection = subsection;
			      }	
			      boolean isOverridden = (Boolean)(en.getMethod("isOverridden")).invoke(v);
			      gen.writeStartObject();																				//					{
			      gen.writeStringField(key,valueAsString);															//							"key": "value"	
			      gen.writeStringField("description", valueDescription);												//						,"description":"description"
			      gen.writeStringField("type",type.getSimpleName());													//						,"type":"type"
			      gen.writeBooleanField("editable",isEditable);													//						,"editable":"true|false"
			      gen.writeBooleanField("visible",isVisible);													//						,"visible":"true|false"
			      gen.writeBooleanField("overridden",isOverridden);													//						,"overridden":"true|false"
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
		ImmutableCollection<String> keys = CONFIGURATION_SECTIONS.keySet();  
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jfactory = mapper.getJsonFactory();
		StringWriter sw = new StringWriter();	
		try{
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			gen.writeStartArray();	
			for (String v: keys){
				String st = dumpConfigurationAsJson(v);
				ObjectMapper op= new ObjectMapper();
				JsonNode p = op.readTree(st);
				Logger.debug("OBJECT:" + p.toString());
				Logger.debug("STRING:" + st);
				//JsonParser jp = jfactory.createJsonParser(st);
				gen.writeTree(p);
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
		ImmutableCollection<String> keys = CONFIGURATION_SECTIONS.keySet(); 
		StringBuilder sb = new StringBuilder();
		for (String v: keys){
			sb.append(dumpConfiguration(v));
			sb.append("\n");
		}
		return sb.toString();
	}//dumpConfiguration()
	

	
	public static String dumpConfiguration(String section) {
		Class en = CONFIGURATION_SECTIONS.get(section);
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
	
	public static String dumpConfigurationSectionAsFlatJson(String section){
		Class en = CONFIGURATION_SECTIONS.get(section);
		try {
			JsonFactory jfactory = new JsonFactory();
			StringWriter sw = new StringWriter();
			String enumDescription = "";			
			JsonGenerator gen = jfactory.createJsonGenerator(sw);
			gen.writeStartArray();	
			EnumSet values = EnumSet.allOf( en );
			for (Object v : values) {
				  String key=(String) (en.getMethod("getKey")).invoke(v);
				  
				  
				  boolean isVisible=(Boolean)(en.getMethod("isVisible")).invoke(v);
				  String valueAsString;
				  if (isVisible) valueAsString=(String) (en.getMethod("getValueAsString")).invoke(v);
				  else valueAsString = "--HIDDEN--";
				  boolean isEditable=(Boolean)(en.getMethod("isEditable")).invoke(v);
			      boolean isOverridden = (Boolean)(en.getMethod("isOverridden")).invoke(v);
				  String valueDescription=(String) (en.getMethod("getValueDescription")).invoke(v);
				  Class type = (Class) en.getMethod("getType").invoke(v);
				  
			      gen.writeStartObject();																				//					{
			      gen.writeStringField("key", key);	
			      gen.writeStringField("value",valueAsString);
			      gen.writeStringField("description", valueDescription);												//						,"description":"description"
			      gen.writeStringField("type",type.getSimpleName());													//						,"type":"type"
			      gen.writeBooleanField("editable", isEditable);
			      gen.writeBooleanField("overridden", isOverridden);
			      gen.writeEndObject();																					//					}
			}
			if (gen.getOutputContext().inArray()) gen.writeEndArray();													//				]
			gen.close();
			return sw.toString();
		} catch (Exception e) {
			Logger.error("Cannot generate a json for "+ en.getSimpleName()+" Enum. Is it an Enum that implements the IProperties interface?",e);
		}
		return "{}";
	}//dumpConfigurationSectionAsJson(String)()

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

 	
	public static Object findByKey(String completeKey) throws ConfigurationException {
		String[] splittedKeys=completeKey.split("\\.");
		String section=splittedKeys[0];
		Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
		EnumSet values = EnumSet.allOf( en );
	    for (Object v : values) {
	        try {
	        	String key=StringUtils.join(Arrays.copyOfRange(splittedKeys, 1, splittedKeys.length),".");
				if ( ((String)en.getMethod("getKey").invoke(v)).equalsIgnoreCase(key)  )
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
	 * @throws PushNotInitializedException 
	 * @throws PushSwitchException 
	 * @throws Exception
	 */
	public static void setByKey(Class en,String iKey,Object value) throws ConfigurationException {
		Object enumValue = findByKey(en,iKey);
		try {
			en.getMethod("setValue",Object.class).invoke(enumValue,value);
		}catch (Exception e) {
			if (e.getCause() instanceof IllegalStateException) throw new IllegalStateException(e.getCause());
			if (e.getCause() instanceof PushSwitchException) throw (PushSwitchException) e.getCause();
			if (e.getCause() instanceof PushNotInitializedException) throw (PushNotInitializedException) e.getCause();
			if (e.getCause() instanceof PushInvalidApiKeyException) throw (PushInvalidApiKeyException) e.getCause();
			throw new ConfigurationException ("Invalid key (" +iKey+ ") or value (" +value+")"  ,e );
		}
	}	//setByKey
	
	public static void override(String completeKey,Object value) throws ConfigurationException  {
		Object enumValue = findByKey(completeKey);
		try {
			String[] splittedKeys=completeKey.split("\\.");
			String section=splittedKeys[0];
			Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
			en.getMethod("override",Object.class).invoke(enumValue,value);
		} catch (Exception e) {
			throw new ConfigurationException ("Invalid key -" +completeKey+ "- or value -" +value+"-"  ,e );
		}
	}
	  
	
	public static void setVisible(String completeKey, Boolean value) throws ConfigurationException {
		Object enumValue = findByKey(completeKey);
		try {
			String[] splittedKeys=completeKey.split("\\.");
			String section=splittedKeys[0];
			Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
			en.getMethod("setVisible",boolean.class).invoke(enumValue,value);
		} catch (Exception e) {
			Logger.error("Invalid key -" +completeKey+ "- or value -" +value+"-",e);
			throw new ConfigurationException ("Invalid key -" +completeKey+ "- or value -" +value+"-"  ,e );
		}
	}
	 
	public static void setEditable(String completeKey, Boolean value) throws ConfigurationException {
		Object enumValue = findByKey(completeKey);
		try {
			String[] splittedKeys=completeKey.split("\\.");
			String section=splittedKeys[0];
			Class en = PropertiesConfigurationHelper.CONFIGURATION_SECTIONS.get(section);
			en.getMethod("setEditable",boolean.class).invoke(enumValue,value);
		} catch (Exception e) {
			Logger.error("Invalid key -" +completeKey+ "- or value -" +value+"-",e);
			throw new ConfigurationException ("Invalid key -" +completeKey+ "- or value -" +value+"-"  ,e );
		}
	}
	
}//PropertiesConfigurationHelper
