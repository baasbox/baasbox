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

package com.baasbox.service.sociallogin;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import play.libs.Json;

@JsonIgnoreProperties(ignoreUnknown=true)
public class UserInfo {
	
	private String username;
	private String password;
	private String from;
	private String token;
	private String secret;
	
	private String id;
	private Map<String,String> additionalData ;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public Map<String, String> getAdditionalData() {
		return additionalData;
	}
	public void setAdditionalData(Map<String, String> additionalData) {
		this.additionalData = additionalData;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	
	
	
	public void addData(String key,String value){
		if(this.additionalData == null){
			this.additionalData = new HashMap<String, String>();
		}
		this.additionalData.put(key, value);
	}
	public static UserInfo fromJson(String json) {
		return Json.fromJson(Json.parse(json), UserInfo.class);
	}
	public String toJson() {
		return Json.stringify(Json.toJson(this));
	}
	
	
}
