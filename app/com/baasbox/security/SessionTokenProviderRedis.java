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

package com.baasbox.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;

import play.mvc.Http;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.BBJson;
import com.baasbox.util.BBJson.ObjectMapperExt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.typesafe.plugin.RedisPlugin;

public class SessionTokenProviderRedis implements ISessionTokenProvider {
	

	protected int expiresInSeconds=0; //default expiration of session tokens
	
	private static SessionTokenProviderRedis me; 
	
	private static ISessionTokenProvider initialize(){
		BaasBoxLogger.debug("Initializing SessionTokenProviderRedis...");
		if (me==null) me=new SessionTokenProviderRedis();
		BaasBoxLogger.debug("...done");
		return me;
	}
	public static ISessionTokenProvider getSessionTokenProvider(){
		return initialize();
	}
	
	public static void destroySessionTokenProvider(){
		me=null;
	}
	
	//constructor
	public SessionTokenProviderRedis(){
		setTimeout(expiresInSeconds);
	};	
	
	public void setTimeout(long timeoutInMilliseconds){
		this.expiresInSeconds=(int)(timeoutInMilliseconds / 1000);
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("New session timeout: " + timeoutInMilliseconds + " ms");
	}	//setTimeout
	
	@Override
	public ImmutableMap<SessionKeys, ? extends Object> setSession(String AppCode, String username,	String password) {
		String redisToken= new StringBuilder()
						.append(Http.Context.current().args.get("appcode"))
						.append(":session:")
						.append(username)
						.append(":")
						.append(UUID.randomUUID().toString())
						.toString();
		String baasboxToken=baasboxizeTheToken(redisToken);
		ImmutableMap<SessionKeys, ? extends Object> info = ImmutableMap.of
				(SessionKeys.APP_CODE, AppCode, 
						SessionKeys.TOKEN, baasboxToken,
						SessionKeys.USERNAME, username,
						SessionKeys.PASSWORD,password,
						SessionKeys.EXPIRE_TIME,(new Date()).getTime()+expiresInSeconds);
		ObjectMapperExt mapper = BBJson.mapper();
		Jedis j=null;
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		try {
			String mapAsJson = mapper.writeValueAsString(info);
			j = p.getResource();
			if (expiresInSeconds!=0) {
				j.set(redisToken, mapAsJson);
				j.expire(redisToken,expiresInSeconds);
			} else {
				j.set(redisToken, mapAsJson);
			}
		} catch (Exception e) {
			BaasBoxLogger.error("Error inserting new token in Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
		return info;
	}

	@Override
	public ImmutableMap<SessionKeys, ? extends Object> getSession(String baasboxToken) {
		if (isExpired(baasboxToken)){
			return null;
		}
		Jedis j=null;
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		String redisToken = redisizeTheToken(baasboxToken);
		try {
			j = p.getResource();
			String mapAsJsonString = j.get(redisToken);
			ObjectMapperExt mapper = BBJson.mapper();
			ObjectNode mapAsJson = (ObjectNode) mapper.readTree(mapAsJsonString);
			ImmutableMap<SessionKeys, ? extends Object> info = ImmutableMap.of
					(SessionKeys.APP_CODE, mapAsJson.get(SessionKeys.APP_CODE.toString()).asText(), 
							SessionKeys.TOKEN, mapAsJson.get(SessionKeys.TOKEN.toString()).asText(),
							SessionKeys.USERNAME, mapAsJson.get(SessionKeys.USERNAME.toString()).asText(),
							SessionKeys.PASSWORD,mapAsJson.get(SessionKeys.PASSWORD.toString()).asText(),
							SessionKeys.EXPIRE_TIME,(new Date()).getTime()+expiresInSeconds);
			if (expiresInSeconds!=0) {
				j.set(redisToken.toString(),  mapper.writeValueAsString(info));
				j.expire(redisToken.toString(),expiresInSeconds);
			} else {
				j.set(redisToken.toString(),  mapper.writeValueAsString(info));
			}
			return info;
		} catch (IOException e) {
			BaasBoxLogger.error("Error retrieving token from Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
	}

	@Override
	public void removeSession(String baasboxToken) {
		String redisToken=redisizeTheToken(baasboxToken);
		Jedis j=null;
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		try {
			j = p.getResource();
			j.del(redisToken);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionTokenProviderRedis: " + baasboxToken + " removed");
		} catch (Exception e) {
			BaasBoxLogger.error("Error deleting token from Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
	}

	@Override
	public Enumeration<String> getTokens() {
		Jedis j=null;
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		try {
			j = p.getResource();
			Set<String> ret = j.keys(Http.Context.current().args.get("appcode")+":session:*");
			ret=ret.stream().map(x->baasboxizeTheToken(x)).collect(Collectors.toSet());
			return new Vector<String>(ret).elements();
		} catch (Exception e) {
			BaasBoxLogger.error("Error retrieving tokens from Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
	}
	
	private boolean isExpired(String baasboxToken){
		String redisToken = redisizeTheToken(baasboxToken);
		Jedis j=null;
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		try {
			j = p.getResource();
			return !j.exists(redisToken);
		} catch (Exception e) {
			BaasBoxLogger.error("Error checking token  using Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
	}
	@Override
	public ImmutableMap<SessionKeys, ? extends Object> getCurrent() {
		String token = (String) Http.Context.current().args.get("token");
		if (token != null) return getSession(token);
		else return null;
	}
	
	@Override
	public List<ImmutableMap<SessionKeys, ? extends Object>> getSessions(String username) {
		JedisPool p = play.Play.application().plugin(RedisPlugin.class).jedisPool();
		final Jedis j=p.getResource();
		try { 
			Set<String> keys = j.keys((new StringBuilder())
					.append(Http.Context.current().args.get("appcode"))
					.append(":session:")
					.append(username)
					.append(":*")
					.toString()
					);
			ObjectMapperExt mapper = BBJson.mapper();
			Stream<ImmutableMap<SessionKeys, ? extends Object>> toRet = keys.stream().map(x->{
				ImmutableMap<SessionKeys, ? extends Object> info = null;
				try {
					JsonNode mapAsJson = mapper.readTree(j.get(x));
					info = ImmutableMap.of
							(SessionKeys.APP_CODE, mapAsJson.get(SessionKeys.APP_CODE.toString()).asText(), 
									SessionKeys.TOKEN, mapAsJson.get(SessionKeys.TOKEN.toString()).asText(),
									SessionKeys.USERNAME, mapAsJson.get(SessionKeys.USERNAME.toString()).asText(),
									SessionKeys.PASSWORD,mapAsJson.get(SessionKeys.PASSWORD.toString()).asText(),
									SessionKeys.EXPIRE_TIME,(new Date()).getTime()+expiresInSeconds);
				} catch (Exception e) {
					BaasBoxLogger.error("Unable to deserialize session value from Redis",e);
				}
				return info;
			});
			return toRet.collect(Collectors.toList());
		} catch (Exception e) {
			BaasBoxLogger.error("Error checking token existance using Redis", e);
			throw new RuntimeException("Redis problem",e);
		} finally {
			if (j!=null) p.returnResource(j);
		}
	}
	
	private String redisizeTheToken(String baasboxToken){
		try {
			return new String(Base64.decodeBase64(baasboxToken), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// is not UTF-8 supported??? REALLY??
			BaasBoxLogger.error("is not UTF-8 supported?", e);
			throw new RuntimeException(e);
		}
	}
	
	private String baasboxizeTheToken(String redisToken){
		return new String(Base64.encodeBase64(redisToken.toString().getBytes()));
	}
}
