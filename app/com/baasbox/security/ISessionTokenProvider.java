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

import java.util.Enumeration;
import java.util.List;

import com.google.common.collect.ImmutableMap;

public interface ISessionTokenProvider {
		public SessionObject setSession(String AppCode, String username, String Password);
		public SessionObject getSession(String token);
		public SessionObject getCurrent();
		public void removeSession(String token);
		public void setTimeout(long timeoutInMilliseconds);
		public Enumeration<String> getTokens();
		public List<SessionObject> getSessions(String username);		
		
}
