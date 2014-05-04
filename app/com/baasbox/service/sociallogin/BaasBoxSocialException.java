package com.baasbox.service.sociallogin;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class  BaasBoxSocialException extends Exception {

	protected JsonNode error;

	public BaasBoxSocialException() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected  BaasBoxSocialException(JsonNode error) {
		this.error=error;
	}
	
	
	public BaasBoxSocialException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxSocialException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxSocialException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
	
	public abstract String getMessage();
	public abstract String getErrorType();
	public abstract String getErrorCode();
	public abstract String getErrorSubCode();
	public  JsonNode getError(){
		return this.error;
	}
	
	
	
	

}
