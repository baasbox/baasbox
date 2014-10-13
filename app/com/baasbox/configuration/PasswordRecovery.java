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

import org.apache.commons.lang3.BooleanUtils;

import com.baasbox.configuration.index.IndexPasswordRecoveryConfiguration;


public enum PasswordRecovery implements IProperties{
	EMAIL_TEMPLATE_TEXT("email.template.text", "The template (text format) of the email to send to the user when they request a password reset. Please ensure that you have written the keyword $link$ inside the text. This keyword will be replaced with the link that the user has to click on to start the password recovery process.", String.class),
	EMAIL_TEMPLATE_HTML("email.template.html", "The template (html format) of the email to send to the user when they request a password reset. Please ensure that you have written the keyword $link$ inside the text. This keyword will be replaced with the link that the user has to click on to start the password recovery process.", String.class),	
	EMAIL_FROM("email.from", "The name and address to specify in the from field of the email to send. Example: example.com <email_from@example.com>", String.class),
	EMAIL_SUBJECT("email.subject", "The subject of the email to send.", String.class),
	EMAIL_EXPIRATION_TIME("email.expiration.time", "Minutes before the reset code expires.", Integer.class),

	PAGE_HTML_TEMPLATE("page.html.template","The HTML template of the reset password page. You coud use the following placeholder: $user_name$, $link$, $error$, $password$, $repeat_password$, $application_name$.", String.class),
	PAGE_HTML_FEEDBACK_TEMPLATE("page.html.feedback.template","The HTML template feedback page. It should contain the $error$ and $message$ placeholders.", String.class),
	
	NETWORK_SMTP_HOST("network.smtp.host", "IP ADDRESS or fully qualified name of the SMTP server.", String.class),
	NETWORK_SMTP_PORT("network.smtp.port", "The TCP port of the SMTP server.", Integer.class),
	NETWORK_SMTP_SSL("network.smtp.ssl", "Enable or disable the SSL protocol for the SMTP server.", Boolean.class),
	NETWORK_SMTP_TLS("network.smtp.tls", "Enable or disable the TLS protocol for the SMTP server.", Boolean.class),
	NETWORK_SMTP_AUTHENTICATION("network.smtp.authentication", "Set to TRUE if the SMTP server requires authentication.", Boolean.class),
	NETWORK_SMTP_USER("network.smtp.user", "The username required by the SMTP server when authentication is required. Used only if network.smtp.authentication is set to TRUE", String.class),
	NETWORK_SMTP_PASSWORD("network.smtp.password", "The password required by the SMTP server if it requires authentication. Used only if network.smtp.authentication is set to TRUE", String.class);
	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
  
	//override 
	private boolean 					 editable=true;
	private boolean						 visible=true;
	private Object 						 overriddenValue=null;
	private boolean						 overridden=false;
	
	PasswordRecovery(final String iKey, final String iDescription, final Class<?> iType, 
		    final IPropertyChangeCallback iChangeAction) {
		    this(iKey, iDescription, iType);
		    changeCallback = iChangeAction;
	}

	PasswordRecovery(final String iKey, final String iDescription, final Class<?> iType) {
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

	    try{
	    	if (newValue != null && !newValue.toString().equals(""))
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
	    }catch (Exception e){
	    	Logger.warn(newValue + " value is invalid for key " + key + "\nNULL will be stored");
	    }
	    if (changeCallback != null) changeCallback.change(getValue(), newValue);		
		IndexPasswordRecoveryConfiguration idx;
		try {
			idx = new IndexPasswordRecoveryConfiguration();
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
		IndexPasswordRecoveryConfiguration idx;
		try {
			idx = new IndexPasswordRecoveryConfiguration();
			return idx.get(key);
		} catch (Exception e) {
			Logger.error("Could not retrieve key " + key, e);
		}
		return null;
	}

	@Override
	public boolean getValueAsBoolean() {
	    Object v = getValue();
	    if (v==null) return false;
	    return v instanceof Boolean ? BooleanUtils.isTrue((Boolean) v) : BooleanUtils.toBoolean(v.toString());
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
		return "Configurations for password recovery related properties"; 
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
