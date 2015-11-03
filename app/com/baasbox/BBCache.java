package com.baasbox;

import play.cache.Cache;
import play.mvc.Http;



public class BBCache {

	private static final String UUID_KEY=":uuid:";
	private static final String GLOBAL_CACHE_KEY_FORMAT="%s:global:cache:%s";
	private static final String LOCAL_CACHE_KEY_FORMAT="%s:%s:cache:%s";
	public static final int UUID_TIMEOUT=0; //seconds, 0=unlimited
	private static final String TAG_KEY=":tag:";
	public static final int TAG_TIMEOUT=120;
  private static final String TWITTER_KEY = ":twitter:";

	public static String getUUIDKey(){
		return (new StringBuilder())
				.append(Http.Context.current().args.get("appcode"))
				.append(UUID_KEY)
				.toString(); 
	}
	
  public static String getTwitterKey() {
    return (new StringBuilder())
      .append(Http.Context.current().args.get("appcode"))
      .append(TWITTER_KEY)
      .toString();
  }

  public static void setTwitterToken(String uuid, String token) {
    Cache.set(getTwitterKey() + uuid, token);
  }

	public static String getTagKey(){
		return (new StringBuilder())
				.append(Http.Context.current().args.get("appcode"))
				.append(TAG_KEY)
				.toString(); 
	}
	
	public static void cacheUUIDtoRID(String uuid, String rid){
		if (!rid.startsWith("#-")) //in case of transaction, RIDs are like #-2:1
			Cache.set(getUUIDKey()+uuid, rid);
	}

	public static void cacheUUIDtoRID(String uuid, String rid, int secondsToExpiration){
		Cache.set(getUUIDKey()+uuid, rid, secondsToExpiration);
	}
	
	public static String getRidFromUUID(String uuid){
		return (String) Cache.get(getUUIDKey()+uuid);
	}
	
	public void removeUUID(String uuid){
		Cache.remove(getUUIDKey()+uuid);
	}
	
	public static void setValueInLocalCache(String username,String key,Object value,Integer ttl){
		String appcode = (String) Http.Context.current().args.get("appcode");
		Cache.set(String.format(LOCAL_CACHE_KEY_FORMAT,appcode,username,key), value,ttl);
	}
	
	public static  void setValueInGlobalCache(String key,Object value,Integer ttl){
		String appcode = (String) Http.Context.current().args.get("appcode");
		Cache.set(String.format(GLOBAL_CACHE_KEY_FORMAT,appcode,key), value,ttl);
	}
	
	public static Object getValueFromLocalCache(String username,String key){
		String appcode = (String) Http.Context.current().args.get("appcode");
		return Cache.get(String.format(LOCAL_CACHE_KEY_FORMAT,appcode,username,key));
	}
	
	public static Object getValueFromGlobalCache(String key){
		String appcode = (String) Http.Context.current().args.get("appcode");
		return Cache.get(String.format(GLOBAL_CACHE_KEY_FORMAT,appcode,key));
	}

	public static void removeValueFromLocalCache(String username, String key) {
		String appcode = (String) Http.Context.current().args.get("appcode");
		Cache.remove(String.format(LOCAL_CACHE_KEY_FORMAT,appcode,username,key));
	}
	
	public static void removeValueFromGlobalCache(String key){
		String appcode = (String) Http.Context.current().args.get("appcode");
		Cache.remove(String.format(GLOBAL_CACHE_KEY_FORMAT,appcode,key));
	}
}
