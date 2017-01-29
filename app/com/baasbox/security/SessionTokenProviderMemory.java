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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import play.libs.Akka;
import play.mvc.Http;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Cancellable;

import com.baasbox.service.logging.BaasBoxLogger;
import com.google.common.collect.ImmutableMap;

public class SessionTokenProviderMemory extends SessionTokenProviderAbstract {
	
	protected class SessionCleaner implements Runnable{
        @Override
        public void run() {
        	BaasBoxLogger.info("Session Cleaner: started");
        	Enumeration<String> tokens=getTokens();
        	long totalTokens=0;
        	long removedTokens=0;
        	while (tokens.hasMoreElements()){
        		totalTokens++;
        		if (isExpired(tokens.nextElement())) removedTokens++;
        	}
        	BaasBoxLogger.info("Session cleaner: tokens: " + totalTokens + " - removed: " + removedTokens);
        	BaasBoxLogger.info("Session cleaner: finished");
        }
    }

	protected final static ConcurrentHashMap<String,SessionObject> sessions=new ConcurrentHashMap<String, SessionObject>();
	protected long expiresInMilliseconds=0; //default expiration of session tokens
	protected long  sessionClenanerLaunchInMinutes=60; //the session cleaner will be launch each x minutes.
	
	private Cancellable sessionCleaner=null;
	private static SessionTokenProviderMemory me; 
	
	private static ISessionTokenProvider initialize(){
		if (me==null) me=new SessionTokenProviderMemory();
		return me;
	}
	public static ISessionTokenProvider getSessionTokenProvider(){
		return initialize();
	}
	
	public static void destroySessionTokenProvider(){
		if (me!=null && me.sessionCleaner!=null) {
			me.sessionCleaner.cancel();
			BaasBoxLogger.info("Session Cleaner: cancelled");
		}
		me=null;
	}
	
	public SessionTokenProviderMemory(){
		setTimeout(expiresInMilliseconds);
		startSessionCleaner(sessionClenanerLaunchInMinutes*60000); //converts minutes in milliseconds
	};	
	
	public void setTimeout(long timeoutInMilliseconds){
		this.expiresInMilliseconds=timeoutInMilliseconds;
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("New session timeout: " + timeoutInMilliseconds + " ms");
	}	//setTimeout
	
	@Override
	public SessionObject setSession(String AppCode, String username,String password) {
		UUID token = UUID.randomUUID();
		long now = (new Date()).getTime();
		SessionObject info = SessionObject.create(
						token.toString(),
						AppCode,
						username,
						password,
						now,
						now+expiresInMilliseconds);
		sessions.put(token.toString(), info);
		return info;
	}

	@Override
	public SessionObject getSession(String token) {
		if (isExpired(token)){
			return null;
		}
		SessionObject info = sessions.get(token);
		if (info==null){
			BaasBoxLogger.warn("Token {} is not valid and will be revoked.", token);
			removeSession(token);
			return null;
		}
		info.setExpirationTime( (new Date().getTime())+expiresInMilliseconds);
		return info;
	}

	@Override
	public void removeSession(String token) {
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionTokenProvider: " + token + " removed");
		sessions.remove(token);

	}

	@Override
	public Enumeration<String> getTokens() {
		return sessions.keys();
	}
	
	private boolean isExpired(String token){
		SessionObject info = sessions.get(token);
		if (info==null) return true;
		if (expiresInMilliseconds!=0 && (new Date()).getTime()>(Long)info.getExpirationTime()){
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
	
	@Override
	public List<SessionObject> getSessions(String username) {
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("SessionTokenProvider (in memory) - getSessions method started for user {}",username);
		Stream<SessionObject> 
			values = sessions
						.values()
						.stream()
						.filter(x->x.getUsername().equals(username));
		List<SessionObject> toRet = values.collect(Collectors.toList());
		return toRet;
	}
	
	@Override
	public SessionObject getCurrent() {
		String token = (String) Http.Context.current().args.get("token");
		if (token != null) return sessions.get(token);
		else return null;
	}
}
