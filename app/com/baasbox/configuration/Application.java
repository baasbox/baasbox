package com.baasbox.configuration;

import play.Logger;

import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.SessionTokenProvider;


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
	public void setValue(Object newValue) {
	    Object parsedValue=null;

	    Logger.debug("New setting value, key: " + this.key + ", type: "+ this.type + ", new value: " + newValue);
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
		}
	}

	@Override
	public Object getValue() {
		IndexApplicationConfiguration idx;
		try {
			idx = new IndexApplicationConfiguration();
			Logger.debug("getting "+key+" from index");
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

}
