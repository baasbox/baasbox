package com.baasbox.service.scripting.cache;

import org.apache.commons.lang3.StringUtils;

public enum CacheType {
	GLOBAL,LOCAL;
	
	public static CacheType fromString(String from){
		com.google.common.base.Preconditions.checkArgument(StringUtils.isNotBlank(from));
		switch(from.toLowerCase()){
		case "global": return GLOBAL;
		case "local": return LOCAL;
		default:throw new IllegalArgumentException(String.format("Unrecognized cache type %s",from));
		}
	}
}
