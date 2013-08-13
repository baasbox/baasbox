/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox;

import play.Configuration;
import play.Play;

public class BBConfiguration implements IBBConfigurationKeys {
	public static Configuration configuration = Play.application().configuration();
	
	@Deprecated
	public static String getRealm(){
		return configuration.getString(REALM);
	}
	
	public static String getBaasBoxUsername(){
		return configuration.getString(ANONYMOUS_USERNAME);
	}
	
	public static String getBaasBoxPassword(){
		return configuration.getString(ANONYMOUS_PASSWORD);
	}
	public static String getApiVersion(){
		return configuration.getString(API_VERSION);
	}
	public static String getDBDir(){
		return configuration.getString(DB_PATH);
	}
	
	public static Boolean getWrapResponse(){
		return Boolean.valueOf(configuration.getString(WRAP_RESPONSE));
	}
	
	public static String getAPPCODE() {
		return configuration.getString(APP_CODE);
	}
	
}
