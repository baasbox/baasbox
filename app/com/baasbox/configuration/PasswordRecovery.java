package com.baasbox.configuration;

import play.Logger;


public enum PasswordRecovery implements IProperties{
	EMAIL_TEMPLATE_TEXT("email.template.text", "The template (in text format) of the email to be send to the user when he/she requests a password reset", String.class),
	EMAIL_TEMPLATE_HTML("email.template.html", "The template (in html format) of the email to be send to the user when he/she requests a password reset", String.class),	
	NETWORK_SMTP_ENABLE("network.smtp.enable", "Enable or disable the use of a SMTP server to send the reset password email", Boolean.class),
	NETWORK_SMTP_HOST("network.smtp.host", "IP ADDRESS or fully qualified name of the SMTP server. Used only if network.smtp.enable is set to TRUE", String.class),
	NETWORK_SMTP_PORT("network.smtp.port", "The TCP port of the SMTP server. Used only if network.smtp.enable is set to TRUE", Integer.class),
	NETWORK_SMTP_SSL("network.smtp.ssl", "Enable or disable the SSL protocol for the SMTP server. Used only if network.smtp.enable is set to TRUE", Boolean.class),
	NETWORK_SMTP_AUTHENTICATION("network.smtp.authentication", "Set to TRUE if the SMTP server requires authentication. Used only if network.smtp.enable is set to TRUE", Boolean.class),
	NETWORK_SMTP_USER("network.smtp.user", "The username required by the SMTP server if it requires authentication. Used only if network.smtp.authentication is set to TRUE", String.class),
	NETWORK_SMTP_PASSWORD("network.smtp.password", "The password required by the SMTP server if it requires authentication. Used only if network.smtp.authentication is set to TRUE", String.class);

	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
  
  
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
		return "Configurations for password recovery related properties"; 
	}

}
