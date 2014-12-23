package com.baasbox;

import play.cache.Cache;



public class BBCache {

	private static String UUID_KEY="uuid.";
	public static int UUID_DEFAULT_TIMEOUT=3600; //seconds

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
