package com.baasbox.configuration;

import play.Logger;


/***
 * Internal (intended to be used internally by BaasBox) keys and settings
 * @author Claudio Tesoriero
 *
 */
public enum Internal implements IProperties{
	DB_VERSION("db.version", "The BaasBox database level. I.E. for BaasBox 0.5.7, this is 0.5.7. It is useful to know when the BaasBox core has been upgraded and the db schema needs to be an upgrade too", String.class),
	INSTALLATION_ID("installation.id", "Unique id for the current DB. This could be useful to track changes on the db. The id is created when db is created", String.class);
	
	
	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
  
  
	Internal(final String iKey, final String iDescription, final Class<?> iType, 
		    final IPropertyChangeCallback iChangeAction) {
		    this(iKey, iDescription, iType);
		    changeCallback = iChangeAction;
	}

	Internal(final String iKey, final String iDescription, final Class<?> iType) {
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
	    IndexInternalConfiguration idx;
		try {
			idx = new IndexInternalConfiguration();
			idx.put(key, parsedValue);
		} catch (Exception e) {
			Logger.error("Could not store key " + key, e);
		}
	}

	@Override
	public Object getValue() {
		IndexInternalConfiguration idx;
		try {
			idx = new IndexInternalConfiguration();
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
		return "Internal <key/value>, intended for internal usage only"; 
	}

}
