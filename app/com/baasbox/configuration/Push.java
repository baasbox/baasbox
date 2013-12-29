package com.baasbox.configuration;

import play.Logger;


public enum Push implements IProperties{
	PUSH_SANDBOX_ENABLE("push.sandbox.enable", "The value for verify if BaasBox need to contact Sandbox server or Production server", Boolean.class),
	PUSH_APPLE_TIMEOUT("push.apple.timeout", "The timeout of push notification for device Apple", Integer.class),
	SANDBOX_ANDROID_API_KEY("sandbox.android.api.key", "The key for send push notifications to Android device in sandbox mode", String.class),
	SANDBOX_IOS_CERTIFICATE("sandbox.ios.certificate", "The path of Apple certificate in sandbox mode", String.class),	
	SANDBOX_IOS_CERTIFICATE_PASSWORD("sandbox.ios.certificate.password", "The password of Apple certificate in sandbox mode", String.class),
	PRODUCTION_ANDROID_API_KEY("production.android.api.key", "The key for send push notifications to Android device in production mode", String.class),
	PRODUCTION_IOS_CERTIFICATE("production.ios.certificate", "The path of Apple certificate in production mode", String.class),	
	PRODUCTION_IOS_CERTIFICATE_PASSWORD("production.ios.certificate.password", "The password of Apple certificate in production mode", String.class);
	

	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
  
  
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
	public void setValue(Object newValue) {
	    Object parsedValue=null;

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
		}
	}

	@Override
	public Object getValue() {
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
		return "Configurations for push related properties"; 
	}

}
