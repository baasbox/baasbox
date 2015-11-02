package com.baasbox.service.scripting.cache;

import java.io.Serializable;

import com.baasbox.BBCache;

public class CacheAccess {
	
	String username;
	
	public CacheAccess(String username){
		this.username = username;
	}
	
	public void setValue(String cacheType,String key,Object value){
		CacheType type = CacheType.fromString(cacheType);
		if(type.equals(CacheType.LOCAL)){
			BBCache.setValueInLocalCache(this.username,key,value);
		}else{
			BBCache.setValueInGlobalCache(key,value);
		}
	}
	
	public Object getValue(String cacheType,String key){
		CacheType type = CacheType.fromString(cacheType);
		if(type.equals(CacheType.LOCAL)){
			return BBCache.getValueFromLocalCache(this.username,key);
		}else{
			return BBCache.getValueFromGlobalCache(key);
		}
	}
	
	
}
