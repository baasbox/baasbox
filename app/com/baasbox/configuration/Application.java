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

import play.Logger;

import com.baasbox.configuration.index.IndexApplicationConfiguration;
import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.SessionTokenProvider;
import com.google.common.annotations.VisibleForTesting;


public enum Application implements IProperties{
	APPLICATION_NAME("application.name", "The App name", String.class),
	SESSION_TOKENS_TIMEOUT("session_tokens.timeout", "The expiration time of the session tokens (in minutes). WARNING: the admin console refreshes the session token every 5 minutes, if you set a value lower than 5, you may experience disconnection from the console. To disable expiration set it to 0.", Integer.class,
			//this callback function is invoked when the value changes. It sets the timeout for the session tokens
			new IPropertyChangeCallback(){
				public void change(final Object iCurrentValue, final Object iNewValue){
					ISessionTokenProvider stp = SessionTokenProvider.getSessionTokenProvider();
					stp.setTimeout(Integer.parseInt(iNewValue.toString())*60000);
				}
			}),

	NETWORK_HTTP_SSL("network.http.ssl", "Set to TRUE if the BaasBox server is reached via SSL through a reverse proxy.", Boolean.class),	
	NETWORK_HTTP_URL("network.http.url", "The public url of the BaasBox server. I.e. the url used by the App to contact BaasBox, without the protocol prefix (i.e. http://) and PORT", String.class),
	NETWORK_HTTP_PORT("network.http.port", "The public TCP port used by the App to contact BaasBox. Please note: when behind a reverse proxy, this could be different from the port used by BaasBox.", Integer.class);	
	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
	
	//override 
	private  boolean 					 	 editable=true;
	private  boolean						 visible=true;
	private  Object 						 overriddenValue=null;
	private  boolean						 overridden=false;
  
  Application(final String iKey, final String iDescription, final Class<?> iType, 
		    final IPropertyChangeCallback iChangeAction) {
		    this(iKey, iDescription, iType);
		    changeCallback = iChangeAction;
	}

	Application(final String iKey, final String iDescription, final Class<?> iType) {
		    key = iKey;
		    description = iDescription;
		    type = iType;
	}

	@Override
	public void setValue(Object newValue) throws IllegalStateException{
		if (!editable) throw new IllegalStateException("The value cannot be changed");
		_setValue(newValue);
	}

	@Override
	public void _setValue(Object newValue) {
	    Object parsedValue=null;
	    if (Logger.isDebugEnabled()) Logger.debug("New setting value, key: " + this.key + ", type: "+ this.type + ", new value: " + newValue);
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

	    if (changeCallback != null) changeCallback.change(getValue(), newValue);		
		IndexApplicationConfiguration idx;
		try {
			idx = new IndexApplicationConfiguration();
			idx.put(key, parsedValue);
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
		IndexApplicationConfiguration idx;
		try {
			idx = new IndexApplicationConfiguration();
			if (Logger.isDebugEnabled()) Logger.debug("getting "+key+" from index");
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
		return "Configurations for general App(lication) related properties"; 
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
	
	@VisibleForTesting
	public void _setOverridden(boolean value){
		this.overridden=value;
	}

}
