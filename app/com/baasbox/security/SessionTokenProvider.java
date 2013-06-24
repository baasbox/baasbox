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
	protected long expiresInMilliseconds=900000; //default 15 mins
	private Cancellable sessionCleaner=null;
	private static SessionTokenProvider me; 
	
	public static SessionTokenProvider initialize(){
		me=new SessionTokenProvider();
		return me;
	}
	public static SessionTokenProvider getSessionTokenProvider(){
		return me;
	}
	
	public static void destroySessionTokenProvider(){
		SessionTokenProvider me= getSessionTokenProvider();
		if (me!=null && me.sessionCleaner!=null) {
			me.sessionCleaner.cancel();
			Logger.info("Session Cleaner: cancelled");
		}
		me=null;
	}
	
	public SessionTokenProvider(){
		setTimeout(expiresInMilliseconds);
	};	
	
	public void setTimeout(long timeoutInMilliseconds){
		this.expiresInMilliseconds=timeoutInMilliseconds;
		if (sessionCleaner!=null) {
			sessionCleaner.cancel();
			Logger.info("Session Cleaner: cancelled");
		}
		startSessionCleaner(timeoutInMilliseconds);
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
		sessions.remove(token);
		Logger.debug("SessionTokenProvider: " + token + " removed");
	}

	@Override
	public Enumeration<String> getTokens() {
		return sessions.keys();
	}
	
	private boolean isExpired(String token){
		ImmutableMap<SessionKeys, ? extends Object> info = sessions.get(token);
		if (info==null || (new Date()).getTime()>(Long)info.get(SessionKeys.EXPIRE_TIME)){
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
