package com.baasbox.configuration;

import play.Logger;
import org.apache.commons.lang3.BooleanUtils;


public enum PasswordRecovery implements IProperties{
	EMAIL_TEMPLATE_TEXT("email.template.text", "The template (in text format) of the email to be send to the user when he/she requests a password reset. Be sure you've written inside the keyword $link$. This keyword will be replaced with the link that the user has to click to begin the password recovery process.", String.class),
	EMAIL_TEMPLATE_HTML("email.template.html", "The template (in html format) of the email to be send to the user when he/she requests a password reset. Be sure you've written inside the keyword $link$. This keyword will be replaced with the link that the user has to click to begin the password recovery process.", String.class),	
	EMAIL_FROM("email.from", "The name and address to specify in the from field of the email to be send. ex.site_name<email_from@site.xxx>", String.class),
	EMAIL_SUBJECT("email.subject", "The subject of the email to be send.", String.class),
	EMAIL_EXPIRATION_TIME("email.expiration.time", "Minutes before the reset code expires.", Integer.class),

	PAGE_HTML_TEMPLATE("page.html.template","The HTML template of the reset password page. Don't forget to insert the token  $form_template$. This keyword will be replaced with the html form full of text boxes and button for the submission of the new password.", String.class),

	NETWORK_SMTP_HOST("network.smtp.host", "IP ADDRESS or fully qualified name of the SMTP server.", String.class),
	NETWORK_SMTP_PORT("network.smtp.port", "The TCP port of the SMTP server. Used only if network.smtp.enable is set to TRUE", Integer.class),
	NETWORK_SMTP_SSL("network.smtp.ssl", "Enable or disable the SSL protocol for the SMTP server. Used only if network.smtp.enable is set to TRUE", Boolean.class),
	NETWORK_SMTP_TLS("network.smtp.tls", "Enable or disable the TLS protocol for the SMTP server. Used only if network.smtp.enable is set to TRUE", Boolean.class),
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

}
