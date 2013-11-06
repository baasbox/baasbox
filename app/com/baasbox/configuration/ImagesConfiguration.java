package com.baasbox.configuration;

import java.security.InvalidParameterException;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;

import play.Logger;

public enum ImagesConfiguration implements IProperties{
	IMAGE_ALLOWS_AUTOMATIC_RESIZE("image.allows.automatic.resize", "Enable or disable automatic resizing of images", Boolean.class),
	IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS("image.allowed.automatic.resize.formats", "A comma-separated-values list of image size, both in px or in %. Syntax for eache entry: (<width>[|px|%]-<height>[|px|%]|<ratio>%|<width>x<height>). Example: 120px-60px,135px-22%,50%,125-250, in the last case size are in pixels by default", String.class,
		//this callback function is invoked when the value changes. It checks the correctness of the input and raises an error if it is invalid
		new IPropertyChangeCallback(){
			public void change(final Object iCurrentValue, final Object iNewValue){
				//check the correctness of the passed string
				String value=(String)iNewValue;
				if (value.isEmpty()) return;
				String regexp = "(\\d+(px|%)-\\d+(px|%))|(\\d+%($)|\\d+-\\d+)"; 
				//if this is the input: 120px-60px,135px-22%,50%,%,00000000-5px,0px-2,09%-90,120px-60px,135px-22%,50%,%,00000000-5px,0px-2,09%-90,78-25,98%
				//only the first two and the last two are valid
				//used this http://regexpal.com/ to build the regexp
				try {
					RE re = new RE(regexp);
					String[] values = value.split(" ");
					for (String toMatch: values){
						if (!re.match(toMatch.trim())) throw new InvalidParameterException(toMatch + " is not a valid resize format");
					}
				} catch (RESyntaxException e) {
					//the RE constructor could through an exception if the regexp string is invalid
					throw new RuntimeException(regexp + " is not a valid RegExp", e);
				}
			}
		}),
	;

	private final String                 key;
	private final Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;
	
	
	ImagesConfiguration(final String iKey, final String iDescription, final Class<?> iType, 
		    final IPropertyChangeCallback iChangeAction) {
		    this(iKey, iDescription, iType);
		    changeCallback = iChangeAction;
	}

	ImagesConfiguration(final String iKey, final String iDescription, final Class<?> iType) {
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
		return "Configurations for Images related stuffs"; 
	}

}
