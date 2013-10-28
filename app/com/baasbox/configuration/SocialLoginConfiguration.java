package com.baasbox.configuration;

import play.Logger;

public enum SocialLoginConfiguration implements IProperties{
	FACEBOOK_TOKEN("social.facebook.token","Application Token for facebook app",String.class),
	FACEBOOK_SECRET("social.facebook.secret","Application secret for facebook app",String.class),
	FACEBOOK_ENABLED("social.facebook.enabled","Facebook link enable flag",Boolean.class),
	GOOGLE_TOKEN("social.google.token","Application Token for google app",String.class),
	GOOGLE_SECRET("social.google.secret","Application secret for google app",String.class),
	GOOGLE_ENABLED("social.google.enabled","Google link enable flag",Boolean.class),;

	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback      changeCallback;
	
	SocialLoginConfiguration(final String iKey, final String iDescription, final Class<?> iType, 
		    final IPropertyChangeCallback iChangeAction) {
		    this(iKey, iDescription, iType);
		    changeCallback = iChangeAction;
	}

	SocialLoginConfiguration(final String iKey, final String iDescription, final Class<?> iType) {
		    key = iKey;
		    description = iDescription;
		    type = iType;
	}
	
	
	@Override
	public void setValue(Object newValue) {
		Object parsedValue=null;

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
	    IndexSocialLoginConfiguration idx;
		try {
			idx = new IndexSocialLoginConfiguration();
			idx.put(key, parsedValue);
		} catch (Exception e) {
			Logger.error("Could not store key " + key, e);
		}
	}

	@Override
	public Object getValue() {
		IndexSocialLoginConfiguration idx;
		try {
			idx = new IndexSocialLoginConfiguration();
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
		return "Configurations for Social Login related stuffs"; 
	}

}
