package com.baasbox;

import play.cache.Cache;



public class BBCache {

	private static String UUID_KEY="uuid.";
	public static int UUID_TIMEOUT=0; //seconds, 0=unlimited
	public static String TAG_KEY="tag_";
	public static int TAG_TIMEOUT=120;
	

	public static void cacheUUIDtoRID(String uuid, String rid){
		if (!rid.startsWith("#-")) //in case of transaction, just created RIDs are like #-2:1
			Cache.set(UUID_KEY+uuid, rid);
	}

	public static void cacheUUIDtoRID(String uuid, String rid, int secondsToExpiration){
		Cache.set(UUID_KEY+uuid, rid, secondsToExpiration);
	}
	
	public static String getRidFromUUID(String uuid){
		return (String) Cache.get(UUID_KEY+uuid);
	}
	
	public void removeUUID(String uuid){
		Cache.remove(UUID_KEY+uuid);
	}
}
