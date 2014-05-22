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

import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import play.Logger;
import play.libs.Akka;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.Cancellable;

import com.google.common.collect.ImmutableMap;

public class SessionTokenProvider implements ISessionTokenProvider {
	
	protected class SessionCleaner implements Runnable{
        @Override
        public void run() {
        	Logger.info("Session Cleaner: started");
        	Enumeration<String> tokens=getTokens();
        	long totalTokens=0;
        	long removedTokens=0;
        	while (tokens.hasMoreElements()){
        		totalTokens++;
        		if (isExpired(tokens.nextElement())) removedTokens++;
        	}
        	Logger.info("Session cleaner: tokens: " + totalTokens + " - removed: " + removedTokens);
        	Logger.info("Session cleaner: finished");
        }
    }

	protected final static ConcurrentHashMap<String,ImmutableMap<SessionKeys,? extends Object>> sessions=new ConcurrentHashMap<String, ImmutableMap<SessionKeys,? extends Object>>();
	protected long expiresInMilliseconds=0; //default expiration of session tokens
	protected long  sessionClenanerLaunchInMinutes=60; //the session cleaner will be launch each x minutes.
	
	private Cancellable sessionCleaner=null;
	private static SessionTokenProvider me; 
	
	private static ISessionTokenProvider initialize(){
		if (me==null) me=new SessionTokenProvider();
		return me;
	}
	public static ISessionTokenProvider getSessionTokenProvider(){
		return initialize();
	}
	
	public static void destroySessionTokenProvider(){
		if (me!=null && me.sessionCleaner!=null) {
			me.sessionCleaner.cancel();
			Logger.info("Session Cleaner: cancelled");
		}
		me=null;
	}
	
	public SessionTokenProvider(){
		setTimeout(expiresInMilliseconds);
		startSessionCleaner(sessionClenanerLaunchInMinutes*60000); //converts minutes in milliseconds
	};	
	
	public void setTimeout(long timeoutInMilliseconds){
		this.expiresInMilliseconds=timeoutInMilliseconds;
		if (Logger.isDebugEnabled()) Logger.debug("New session timeout: " + timeoutInMilliseconds + " ms");
	}	//setTimeout
	
	@Override
	public ImmutableMap<SessionKeys, ? extends Object> setSession(String AppCode, String username,	String password) {
		UUID token = UUID.randomUUID();
		ImmutableMap<SessionKeys, ? extends Object> info = ImmutableMap.of
				(SessionKeys.APP_CODE, AppCode, 
						SessionKeys.TOKEN, token.toString(),
						SessionKeys.USERNAME, username,
						SessionKeys.PASSWORD,password,
						SessionKeys.EXPIRE_TIME,(new Date()).getTime()+expiresInMilliseconds);
		sessions.put(token.toString(), info);
		return info;
	}

	@Override
	public ImmutableMap<SessionKeys, ? extends Object> getSession(String token) {
		if (isExpired(token)){
			return null;
		}
		ImmutableMap<SessionKeys, ? extends Object> info = sessions.get(token);
		ImmutableMap<SessionKeys, ? extends Object> newInfo	= ImmutableMap.of
				(SessionKeys.APP_CODE, info.get(SessionKeys.APP_CODE),
						SessionKeys.TOKEN, token,
						SessionKeys.USERNAME, info.get(SessionKeys.USERNAME),
						SessionKeys.PASSWORD,info.get(SessionKeys.PASSWORD),
						SessionKeys.EXPIRE_TIME,(new Date()).getTime()+expiresInMilliseconds);	
		sessions.put(token, newInfo);
		return newInfo;
	}

	@Override
	public void removeSession(String token) {
		if (Logger.isDebugEnabled()) Logger.debug("SessionTokenProvider: " + token + " removed");
		sessions.remove(token);

	}

	@Override
	public Enumeration<String> getTokens() {
		return sessions.keys();
	}
	
	private boolean isExpired(String token){
		ImmutableMap<SessionKeys, ? extends Object> info = sessions.get(token);
		if (info==null) return true;
		if (expiresInMilliseconds!=0 && (new Date()).getTime()>(Long)info.get(SessionKeys.EXPIRE_TIME)){
			removeSession(token);
			return true;
		}
		return false;
	}
	
	private void startSessionCleaner(long timeoutInMilliseconds) {
		sessionCleaner = Akka.system().scheduler().schedule(
			    new FiniteDuration(1000, TimeUnit.MILLISECONDS), 
			    new FiniteDuration(timeoutInMilliseconds, TimeUnit.MILLISECONDS), 
			    new SessionCleaner() , 
			    Akka.system().dispatcher()); 
	}
}
