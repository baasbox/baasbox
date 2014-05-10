package com.baasbox.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;

import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.util.ConfigurationFileContainer;


public enum Push implements IProperties{
	PUSH_SANDBOX_ENABLE("push.sandbox.enable", "The value to verify if BaasBox needs to contact the SANDBOX server or the PRODUCTION server", Boolean.class),
	PUSH_APPLE_TIMEOUT("push.apple.timeout", "The timeout for push notifications on Apple devices", Integer.class),
	SANDBOX_ANDROID_API_KEY("sandbox.android.api.key", "The key to send push notifications to Android devices in SANDBOX mode", String.class),
	SANDBOX_IOS_CERTIFICATE("sandbox.ios.certificate", "The Apple certificate in SANDBOX mode", ConfigurationFileContainer.class,new IosCertificateHandler()),
	SANDBOX_IOS_CERTIFICATE_PASSWORD("sandbox.ios.certificate.password", "The password of the Apple certificate in SANDBOX mode", String.class),
	PRODUCTION_ANDROID_API_KEY("production.android.api.key", "The key to send push notifications to Android devices in PRODUCTION mode", String.class),
	PRODUCTION_IOS_CERTIFICATE("production.ios.certificate", "The Apple certificate in PRODUCTION mode", ConfigurationFileContainer.class,new IosCertificateHandler()),	
	PRODUCTION_IOS_CERTIFICATE_PASSWORD("production.ios.certificate.password", "The password of the Apple certificate in PRODUCTION mode", String.class);



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
	public void setValue(Object newValue) throws IllegalStateException{
		if (!editable) throw new IllegalStateException("The value cannot be changed");
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
