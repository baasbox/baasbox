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

import org.apache.commons.lang.StringUtils;

import play.Logger;

import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.exception.ConfigurationException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.PushSwitchException;
import com.baasbox.service.push.providers.GCMServer;
import com.baasbox.util.ConfigurationFileContainer;
import com.fasterxml.jackson.databind.ObjectMapper;


public enum Push implements IProperties	{
	//DEFAULT PROFILE or FIRST
	PROFILE1_PUSH_SANDBOX_ENABLE("profile1.push.sandbox.enable", "The value to verify if BaasBox needs to contact the SANDBOX server or the PRODUCTION server for the first app", Boolean.class),
	PROFILE1_PUSH_APPLE_TIMEOUT("profile1.push.apple.timeout", "The timeout for push notifications on Apple devices for the first app", Integer.class),
	PROFILE1_SANDBOX_ANDROID_API_KEY("profile1.sandbox.android.api.key", "The key to send push notifications to Android devices in SANDBOX mode for the first app", String.class),
	PROFILE1_SANDBOX_IOS_CERTIFICATE("profile1.sandbox.ios.certificate", "The Apple certificate in SANDBOX mode for the first app", ConfigurationFileContainer.class,new IosCertificateHandler()),
	PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD("profile1.sandbox.ios.certificate.password", "The password of the Apple certificate in SANDBOX mode for the first app", String.class),
	PROFILE1_PRODUCTION_ANDROID_API_KEY("profile1.production.android.api.key", "The key to send push notifications to Android devices in PRODUCTION mode for the first app", String.class),
	PROFILE1_PRODUCTION_IOS_CERTIFICATE("profile1.production.ios.certificate", "The Apple certificate in PRODUCTION mode for the first app", ConfigurationFileContainer.class,new IosCertificateHandler()),	
	PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD("profile1.production.ios.certificate.password", "The password of the Apple certificate in PRODUCTION mode for the first app", String.class),
	PROFILE1_PUSH_PROFILE_ENABLE("profile1.push.profile.enable","Enable this profile",Boolean.class),
	
	//SECOND PROFILE
	PROFILE2_PUSH_SANDBOX_ENABLE("profile2.push.sandbox.enable", "The value to verify if BaasBox needs to contact the SANDBOX server or the PRODUCTION server for the second app", Boolean.class),
	PROFILE2_PUSH_APPLE_TIMEOUT("profile2.push.apple.timeout", "The timeout for push notifications on Apple devices for the second app", Integer.class),
	PROFILE2_SANDBOX_ANDROID_API_KEY("profile2.sandbox.android.api.key", "The key to send push notifications to Android devices in SANDBOX mode for the second app", String.class),
	PROFILE2_SANDBOX_IOS_CERTIFICATE("profile2.sandbox.ios.certificate", "The Apple certificate in SANDBOX mode for the second app", ConfigurationFileContainer.class,new IosCertificateHandler()),
	PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD("profile2.sandbox.ios.certificate.password", "The password of the Apple certificate in SANDBOX mode for the second app", String.class),
	PROFILE2_PRODUCTION_ANDROID_API_KEY("profile2.production.android.api.key", "The key to send push notifications to Android devices in PRODUCTION mode for the second app", String.class),
	PROFILE2_PRODUCTION_IOS_CERTIFICATE("profile2.production.ios.certificate", "The Apple certificate in PRODUCTION mode for the second app", ConfigurationFileContainer.class,new IosCertificateHandler()),	
	PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD("profile2.production.ios.certificate.password", "The password of the Apple certificate in PRODUCTION mode for the second app", String.class),
	PROFILE2_PUSH_PROFILE_ENABLE("profile2.push.profile.enable","Enable this profile",Boolean.class),

	
	//THIRD PROFILE
	PROFILE3_PUSH_SANDBOX_ENABLE("profile3.push.sandbox.enable", "The value to verify if BaasBox needs to contact the SANDBOX server or the PRODUCTION server for the third app", Boolean.class),
	PROFILE3_PUSH_APPLE_TIMEOUT("profile3.push.apple.timeout", "The timeout for push notifications on Apple devices for the third app", Integer.class),
	PROFILE3_SANDBOX_ANDROID_API_KEY("profile3.sandbox.android.api.key", "The key to send push notifications to Android devices in SANDBOX mode for the third app", String.class),
	PROFILE3_SANDBOX_IOS_CERTIFICATE("profile3.sandbox.ios.certificate", "The Apple certificate in SANDBOX mode for the third app", ConfigurationFileContainer.class,new IosCertificateHandler()),
	PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD("profile3.sandbox.ios.certificate.password", "The password of the Apple certificate in SANDBOX mode for the third app", String.class),
	PROFILE3_PRODUCTION_ANDROID_API_KEY("profile3.production.android.api.key", "The key to send push notifications to Android devices in PRODUCTION mode for the third app", String.class),
	PROFILE3_PRODUCTION_IOS_CERTIFICATE("profile3.production.ios.certificate", "The Apple certificate in PRODUCTION mode for the third app", ConfigurationFileContainer.class,new IosCertificateHandler()),	
	PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD("profile3.production.ios.certificate.password", "The password of the Apple certificate in PRODUCTION mode for the third app", String.class),
	PROFILE3_PUSH_PROFILE_ENABLE("profile3.push.profile.enable","Enable this profile",Boolean.class);

	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;

	//override 
	private boolean 					 editable=true;
	private boolean						 visible=true;
	private Object 						 overriddenValue=null;
	private boolean						 overridden=false;
  

	 Push(final String iKey, final String iDescription, final Class<?> iType, 
			final IPropertyChangeCallback iChangeAction) {
		this(iKey, iDescription, iType);
		changeCallback = iChangeAction;
	}

	 Push(final String iKey, final String iDescription, final Class<?> iType) {
		 key = iKey;
		 description = iDescription;
		 type = iType;
	}

	@Override
	public void setValue(Object newValue) throws Exception{
		if (!editable) throw new IllegalStateException("The value cannot be changed");
		//the following tests are necessary due the first db initialization
		if((this.key.equals("profile1.push.profile.enable")) || (this.key.equals("profile2.push.profile.enable")) || (this.key.equals("profile3.push.profile.enable"))) {
			if(this.getValue()==null) {
				_setValue(newValue);
				return;
			}
		}
		if((this.key.equals("profile1.push.sandbox.enable")) || (this.key.equals("profile2.push.sandbox.enable")) || (this.key.equals("profile3.push.sandbox.enable"))) {
			if(this.getValue()==null) {
				_setValue(newValue);
				return;
			}
		}
		if (this.key.contains("api.key")) {
			if(this.getValue()==null) {
				_setValue(newValue);
				return;
			}
			else GCMServer.validateApiKey(newValue.toString());
		}
		boolean bNewValue=false;
		switch  (this) {
			case PROFILE1_PUSH_PROFILE_ENABLE:
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(bNewValue){
					if(Push.PROFILE1_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
						if(StringUtils.isEmpty(Push.PROFILE1_SANDBOX_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Sandbox configuration not properly initialized for the first (default) profile. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
					}else //production
						if(StringUtils.isEmpty(Push.PROFILE1_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Production configuration not properly initialized for the first (default) profile. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
				}
				break;
			case PROFILE2_PUSH_PROFILE_ENABLE:
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(bNewValue){
					if(Push.PROFILE2_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
						if(StringUtils.isEmpty(Push.PROFILE2_SANDBOX_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE2_SANDBOX_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Sandbox configuration not properly initialized for profile 2. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
					}else //production
						if(StringUtils.isEmpty(Push.PROFILE2_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Production configuration not properly initialized for profile 2. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
				}
				break;
			case PROFILE3_PUSH_PROFILE_ENABLE:	
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(bNewValue){
					if(Push.PROFILE3_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
						if(StringUtils.isEmpty(Push.PROFILE3_SANDBOX_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE3_SANDBOX_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Sandbox configuration not properly initialized for profile 3. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
					}else //production
						if(StringUtils.isEmpty(Push.PROFILE3_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
								&& (Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE.getValue()==null
								|| StringUtils.isEmpty(Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
						   ) throw new PushNotInitializedException("Production configuration not properly initialized for profile 3. Hint: check if both iOS Certificate and iOS password or Android API Key are set");
				}
				break;
			case PROFILE1_PUSH_SANDBOX_ENABLE:
				if(!(PROFILE1_PUSH_PROFILE_ENABLE.getValueAsBoolean())){
					_setValue(newValue);
					return;
				}
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(!bNewValue){//switch to production mode
					if(StringUtils.isEmpty(Push.PROFILE1_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
							&& (Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValue()==null
							|| StringUtils.isEmpty(Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
					   ) throw new PushSwitchException("");
				}
				else if(StringUtils.isEmpty(Push.PROFILE1_SANDBOX_ANDROID_API_KEY.getValueAsString()) //switch to sandbox mode
						&& (Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValue()==null
						|| StringUtils.isEmpty(Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
				   ) throw new PushSwitchException("");
				break;
			case PROFILE2_PUSH_SANDBOX_ENABLE:
				if(!(PROFILE2_PUSH_PROFILE_ENABLE.getValueAsBoolean())){
					_setValue(newValue);
					return;
				}
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(!bNewValue){//switch to production mode
					if(StringUtils.isEmpty(Push.PROFILE2_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
							&& (Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE.getValue()==null
							|| StringUtils.isEmpty(Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
					   ) throw new PushSwitchException("");
				}
				else if(StringUtils.isEmpty(Push.PROFILE2_SANDBOX_ANDROID_API_KEY.getValueAsString()) //switch to sandbox mode
						&& (Push.PROFILE2_SANDBOX_IOS_CERTIFICATE.getValue()==null
						|| StringUtils.isEmpty(Push.PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
				   ) throw new PushSwitchException("");
				break;
			case PROFILE3_PUSH_SANDBOX_ENABLE:
				if(!(PROFILE3_PUSH_PROFILE_ENABLE.getValueAsBoolean())){
					_setValue(newValue);
					return;
				}				
				bNewValue = Boolean.parseBoolean(newValue.toString());
				if(!bNewValue){//switch to production mode
					if(StringUtils.isEmpty(Push.PROFILE3_PRODUCTION_ANDROID_API_KEY.getValueAsString()) 
							&& (Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE.getValue()==null
							|| StringUtils.isEmpty(Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
					   ) throw new PushSwitchException("");
				}
				else if(StringUtils.isEmpty(Push.PROFILE3_SANDBOX_ANDROID_API_KEY.getValueAsString()) //switch to sandbox mode
						&& (Push.PROFILE3_SANDBOX_IOS_CERTIFICATE.getValue()==null
						|| StringUtils.isEmpty(Push.PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString()))
				   ) throw new PushSwitchException("");
				break;
			
		}
		_setValue(newValue);
	}

	@Override
	public void _setValue(Object newValue) {
		Object parsedValue=null;
		if (Logger.isDebugEnabled()) Logger.debug("Type:"+type+" Setting "+newValue.toString() + "of class: "+newValue.getClass().toString());
		try{
			if (newValue != null)
				if (type == Boolean.class)
					parsedValue = Boolean.parseBoolean(newValue.toString());
				else if (type == Integer.class)
					parsedValue = Integer.parseInt(newValue.toString());
				else if (type == Float.class)
					parsedValue = Float.parseFloat(newValue.toString());
				else if (type == String.class)
					parsedValue = newValue.toString();
				else if (type == ConfigurationFileContainer.class){
					parsedValue = (ConfigurationFileContainer)newValue;
				}
				else
					parsedValue = newValue;
		}catch (Exception e){
			Logger.warn(newValue + " value is invalid for key " + key + "\nNULL will be stored");
		}
		if (changeCallback != null) changeCallback.change(getValue(), newValue);		
		IndexPushConfiguration idx;
		try {

			idx = new IndexPushConfiguration();
			if(type == ConfigurationFileContainer.class && parsedValue!=null){
				ConfigurationFileContainer cfc = (ConfigurationFileContainer)parsedValue;
				ObjectMapper om = new ObjectMapper();
				idx.put(key, om.writeValueAsString(cfc));
			}else{
				idx.put(key, parsedValue);
			}
		} catch (Exception e) {
			Logger.error("Could not store key " + key, e);
			throw new RuntimeException("Could not store key " + key,e);
		}
	}

	@Override
	public Object getValue() {
		if (overridden) return overriddenValue;
		return _getValue();
	}

	@Override
	public Object _getValue() {
		IndexPushConfiguration idx;
		try {

			idx = new IndexPushConfiguration();
			return idx.get(key);
		} catch (Exception e) {
			Logger.error("Could not retrieve key " + key, e);
		}
		return null;
	}

	@Override
	public boolean getValueAsBoolean() {
		Object v = getValue();
		return v instanceof Boolean ? ((Boolean) v).booleanValue() : Boolean.parseBoolean(v.toString());
	}

	@Override
	public String getValueAsString() {
		Object v = getValue();
		return v != null ? v.toString() : null;
	}

	public ConfigurationFileContainer getValueAsFileContainer() {
		Object v = getValue();
		ConfigurationFileContainer result = null;
		if(v!=null){
			ObjectMapper om = new ObjectMapper();
			try {
				result = om.readValue(v.toString(), ConfigurationFileContainer.class);
			} catch (Exception e) {
				e.printStackTrace();
				return result;
			}
		}
		return result;
	}

	@Override
	public int getValueAsInteger() {
		Object v = getValue();
		return (int) (v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString()));
	}

	@Override
	public long getValueAsLong() {
		Object v = getValue();
		return (long) (v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString()));
	}

	@Override
	public float getValueAsFloat() {
		Object v = getValue();
		return (float) (v instanceof Number ? ((Number) v).floatValue() : Float.parseFloat(v.toString()));
	}

	@Override
	public String getKey() {
		return key;
	}
	

	@Override
	public Class<?> getType() {
		return type;
	}

	@Override
	public String getValueDescription() {
		return description;
	}

	public static String getEnumDescription() {
		return "Configurations for push related properties"; 
	}
	
	@Override
	public void override(Object newValue) {
	    Object parsedValue=null;

	    if (Logger.isDebugEnabled()) Logger.debug("New setting value, key: " + this.key + ", type: "+ this.type + ", new value: " + newValue);
	    if (changeCallback != null) changeCallback.change(getValue(), newValue);	
	    if (newValue != null)
	      if (type == Boolean.class)
	    	  parsedValue = Boolean.parseBoolean(newValue.toString());
	      else if (type == Integer.class)
	    	  parsedValue = Integer.parseInt(newValue.toString());
	      else if (type == Float.class)
	    	  parsedValue = Float.parseFloat(newValue.toString());
	      else if (type == String.class)
	    	  parsedValue = newValue.toString();
	      else
	    	  parsedValue = newValue;
	    this.overriddenValue=parsedValue;
	    this.overridden=true;
	    this.editable=false;
	}

	
	@Override
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	@Override
	public boolean isOverridden() {
		return overridden;
	}
	
	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

}
