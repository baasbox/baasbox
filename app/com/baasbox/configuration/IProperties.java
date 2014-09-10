/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.configuration;

import java.io.IOException;

import com.baasbox.service.push.PushNotInitializedException;



public interface IProperties {
	  public void setValue(final Object iValue) throws Exception;
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
