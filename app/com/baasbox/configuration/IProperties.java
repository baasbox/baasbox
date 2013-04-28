package com.baasbox.configuration;



public interface IProperties {
	  public void setValue(final Object iValue);
	  public Object getValue();
	  public boolean getValueAsBoolean();
	  public String getValueAsString();
	  public int getValueAsInteger();
	  public long getValueAsLong();
	  public float getValueAsFloat();
	  public String getKey();
	  public Class<?> getType();
	  public String getValueDescription();
}
