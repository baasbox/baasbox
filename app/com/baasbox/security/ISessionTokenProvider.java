package com.baasbox.security;

import java.util.Enumeration;

import com.google.common.collect.ImmutableMap;

public interface ISessionTokenProvider {
		public ImmutableMap<SessionKeys, ? extends Object> setSession(String AppCode, String username, String Password);
		public ImmutableMap<SessionKeys, ? extends Object> getSession(String token);
		public void removeSession(String token);
		public void setTimeout(long timeoutInMilliseconds);
		public Enumeration<String> getTokens();
}
