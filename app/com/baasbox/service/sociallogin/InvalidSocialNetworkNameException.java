package com.baasbox.service.sociallogin;

public class InvalidSocialNetworkNameException extends RuntimeException {
	
	
	private static final long serialVersionUID = 1L;
	
	public InvalidSocialNetworkNameException(String socialNetworkName){
		super("Social network "+socialNetworkName+ " is not allowed");
	}
}
