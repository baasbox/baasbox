package com.baasbox.security;

import java.io.Serializable;

import play.libs.Crypto;

import com.baasbox.BBConfiguration;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SessionObject implements Serializable{
	private static final long serialVersionUID = 3375056256139536601L;
	
	private String token;
	private String appcode;
	private String username;
	private String password;
	private long startTime;
	private long expirationTime;
	
	private Object lock = new Object();
	
	
	public String getToken() {
		return token;
	}

	public String getAppcode() {
		return appcode;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getExpirationTime() {
		return expirationTime;
	}

	
	public void setExpirationTime(long expirationTime) {
		synchronized (lock) {
			this.expirationTime = expirationTime;
		}
	}

	private SessionObject(String token, String appcode, String username,
			String password, long startTime, long expirationTime) {
		super();
		this.token = token;
		this.appcode = appcode;
		this.username = username;
		this.password = password;
		this.startTime = startTime;
		this.expirationTime = expirationTime;
	}
	
	public static SessionObject create(String token, String appcode, String username,
			String password, long startTime, long expirationTime) {
		return new SessionObject( token,  appcode,  username,
				 password,  startTime,  expirationTime);
	}
	
	public static SessionObject create(ObjectNode sessionJSON){
		return new SessionObject(
				sessionJSON.get(SessionKeys.TOKEN.toString()).asText(), 
				sessionJSON.get(SessionKeys.APP_CODE.toString()).asText(), 
				sessionJSON.get(SessionKeys.USERNAME.toString()).asText(), 
				sessionJSON.get(SessionKeys.PASSWORD.toString()).asText(),
				sessionJSON.get(SessionKeys.START_TIME.toString()).asLong(), 
				sessionJSON.get(SessionKeys.EXPIRE_TIME.toString()).asLong()
		);
	}
	
	public static SessionObject create(String serializedSession){
		if (serializedSession.startsWith("{")){
			return createSessionFromString(serializedSession);
		}else{  //maybe the string contains an encrypted SessionObject
			if (BBConfiguration.getInstance().isSessionEncryptionEnabled()){
				serializedSession=decrypt(serializedSession);
				return createSessionFromString(serializedSession);
			}else{
				BaasBoxLogger.warn("Serialized SessionObject is not valid. Maybe it has been encrypted. Try to activate the sessions encryption with the rigth key");
				return null;
			}
		}
	}

	private static SessionObject createSessionFromString(
			String serializedSession) {
		ObjectNode sessionJSON=null;
		try{
			sessionJSON=(ObjectNode)BBJson.mapper().readTree(serializedSession);
		}catch (Exception e){
			BaasBoxLogger.warn("Serialized SessionObject is not valid");
			return null;
		}
		return new SessionObject(
			sessionJSON.get(SessionKeys.TOKEN.toString()).asText(), 
			sessionJSON.get(SessionKeys.APP_CODE.toString()).asText(), 
			sessionJSON.get(SessionKeys.USERNAME.toString()).asText(), 
			sessionJSON.get(SessionKeys.PASSWORD.toString()).asText(),
			sessionJSON.get(SessionKeys.START_TIME.toString()).asLong(), 
			sessionJSON.get(SessionKeys.EXPIRE_TIME.toString()).asLong()
		);
	}
	
	@Override
	public String toString(){
		String toRet=this.toJSON().toString();
		if (BBConfiguration.getInstance().isSessionEncryptionEnabled()) toRet = encrypt(toRet);
		return toRet;
	}
	
	private static String encrypt(String stringToEncrypt){
		if (BBConfiguration.getInstance().isSessionEncryptionEnabled()){
			return Crypto.encryptAES(stringToEncrypt);
		}else {
			return stringToEncrypt;
		}
	}
	
	private static String decrypt(String stringToDecrypt){
		if (BBConfiguration.getInstance().isSessionEncryptionEnabled()){
			String toRet=null;
			try{
				toRet=Crypto.decryptAES(stringToDecrypt); 
			}catch (Throwable e){
				BaasBoxLogger.warn("Encryption key is not valid for the given SessionObject.");
			}
			return toRet;
		}else{
			return stringToDecrypt;
		}
	}
	
	public ObjectNode toJSON(){
		ObjectNode obj = BBJson.mapper().createObjectNode();
		obj.put(SessionKeys.TOKEN.toString(), this.getToken());
		obj.put(SessionKeys.APP_CODE.toString(), this.getAppcode());
		obj.put(SessionKeys.USERNAME.toString(), this.getUsername());
		obj.put(SessionKeys.PASSWORD.toString(), this.getPassword());
		obj.put(SessionKeys.START_TIME.toString(), this.getStartTime());
		obj.put(SessionKeys.EXPIRE_TIME.toString(), this.getExpirationTime());
		return obj;
	}
	
}
