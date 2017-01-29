package com.baasbox;

import play.cache.Cache;
import play.mvc.Http;



public class BBCache {

	private static final String UUID_KEY=":uuid:";
	public static final int UUID_TIMEOUT=0; //seconds, 0=unlimited
	private static final String TAG_KEY=":tag:";
	public static final int TAG_TIMEOUT=120;
	

	public static String getUUIDKey(){
		return (new StringBuilder())
				.append(Http.Context.current().args.get("appcode"))
				.append(UUID_KEY)
				.toString(); 
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
}
