package com.baasbox.security;

import com.baasbox.BBConfiguration;
import com.typesafe.plugin.RedisPlugin;

public class SessionTokenProviderFactory {
	protected SessionTokenProviderFactory(){}
	
	protected static ISessionTokenProvider getSessionTokenProvider (String className){
		
		switch(className) {
			case "SessionTokenProvider":
				return SessionTokenProviderMemory.getSessionTokenProvider();
		    case "SessionTokenProviderRedis":		      
		    	return SessionTokenProviderRedis.getSessionTokenProvider();
		    default:
		    	throw new IllegalArgumentException(className + " is not a valid Session Token Provider");
		}
	}
	
	public static ISessionTokenProvider getSessionTokenProvider(){
		
		if (BBConfiguration.configuration.getString("redisplugin").equals("enabled"))
			return getSessionTokenProvider ("SessionTokenProviderRedis");
		else
			return getSessionTokenProvider ("SessionTokenProvider");
			
	}
}
