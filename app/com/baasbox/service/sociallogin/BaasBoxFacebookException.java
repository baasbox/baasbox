package com.baasbox.service.sociallogin;

import com.fasterxml.jackson.databind.JsonNode;



public class BaasBoxFacebookException extends BaasBoxSocialException {

	public BaasBoxFacebookException() {
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(JsonNode jsonNode) {
		super(jsonNode);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getMessage() {
		return (error.get("message")==null?"":error.get("message").asText());
	}

	@Override
	public String getErrorType() {
		return (error.get("type")==null?"":error.get("type").asText());
	}

	@Override
	public String getErrorCode() {
		return (error.get("code")==null?"":error.get("code").asText());
	}

	@Override
	public String getErrorSubCode() {
		return (error.get("error_subcode")==null?"":error.get("error_subcode").asText());
	}

}
