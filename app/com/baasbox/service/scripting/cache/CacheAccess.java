package com.baasbox.service.scripting.cache;

import com.baasbox.BBCache;

public class CacheAccess {
	
	String username;
	
	public CacheAccess(String username){
		this.username = username;
	}
	
	public void setValue(String cacheScope,String key,Object value,Integer ttl){
		CacheType type = CacheType.fromString(cacheScope);
		if(type.equals(CacheType.LOCAL)){
			BBCache.setValueInLocalCache(this.username,key,value,ttl);
		}else{
			BBCache.setValueInGlobalCache(key,value,ttl);
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

	public void removeValue(String cacheScope, String key) {
		CacheType type = CacheType.fromString(cacheScope);
		if(type.equals(CacheType.LOCAL)){
			BBCache.removeValueFromLocalCache(this.username,key);
		}else{
			BBCache.removeValueFromGlobalCache(key);
		}
	}
	
	
}
