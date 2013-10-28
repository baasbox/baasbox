package com.baasbox.service.sociallogin;

public class UnsupportedSocialNetworkException extends RuntimeException {
	
	public UnsupportedSocialNetworkException(String message){
		super(message);
	}
}
