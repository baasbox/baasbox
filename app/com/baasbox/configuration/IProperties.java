package com.baasbox.configuration;



public interface IProperties {
	  public void setValue(final Object iValue);
	  /***
	   * internal use only, sets the original value bypassing the editable flag and the overridden value
	   * @param iValue
	   */
	  public void _setValue(final Object iValue);
	  public Object getValue();
	  /***
	   * internal use only, bypass the overridden value returning the real one
	   * @return
	   */
	  public Object _getValue();
	  public boolean getValueAsBoolean();
	  public String getValueAsString();
	  public int getValueAsInteger();
	  public long getValueAsLong();
	  public float getValueAsFloat();
	  public String getKey();
	  public Class<?> getType();
	  public String getValueDescription();
	  public void  override(Object newValue) ;
	  public boolean isVisible();
	  public boolean isEditable();
	  public void setEditable(boolean value);
	  public void setVisible(boolean value);	 
	  public boolean isOverridden();
}
