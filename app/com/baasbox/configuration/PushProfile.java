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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import play.Logger;

import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.util.ConfigurationFileContainer;


public class PushProfile implements IProperties{
	String pushProfileName;
	HashMap<Push,String> pushProfile;

	private  String                 key;
	private  Class<?>               type;
	private String                       description;
	private IPropertyChangeCallback 	 changeCallback = null;

	//override 
	private boolean 					 editable=true;
	private boolean						 visible=true;
	private Object 						 overriddenValue=null;
	private boolean						 overridden=false;
  
	
	public PushProfile(String profilename){
		this.pushProfileName=profilename;
		HashMap<Push,Object> pushProfile = new HashMap<Push,Object>();
		pushProfile.put(Push.PUSH_APPLE_TIMEOUT, 0);
		pushProfile.put(Push.PUSH_SANDBOX_ENABLE, true);
		pushProfile.put(Push.PRODUCTION_ANDROID_API_KEY, "");
		pushProfile.put(Push.PRODUCTION_IOS_CERTIFICATE, "");
		pushProfile.put(Push.PRODUCTION_IOS_CERTIFICATE_PASSWORD, "");
		pushProfile.put(Push.SANDBOX_ANDROID_API_KEY,"");
		pushProfile.put(Push.SANDBOX_IOS_CERTIFICATE,"");
		pushProfile.put(Push.SANDBOX_IOS_CERTIFICATE_PASSWORD,"");	
	}


	@Override
	public void setValue(Object iValue) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void _setValue(Object iValue) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Object getValue() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Object _getValue() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean getValueAsBoolean() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public String getValueAsString() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int getValueAsInteger() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public long getValueAsLong() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public float getValueAsFloat() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Class<?> getType() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getValueDescription() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void override(Object newValue) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isEditable() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void setEditable(boolean value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setVisible(boolean value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public boolean isOverridden() {
		// TODO Auto-generated method stub
		return false;
	}

}
