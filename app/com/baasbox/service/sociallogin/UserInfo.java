package com.baasbox.service.sociallogin;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {
	
	private String username;
	private String password;
	private String from;
	private String token;
	private String secret;
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
	
	
}
