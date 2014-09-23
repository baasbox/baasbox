package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushBadgeFormatException extends BaasBoxPushException {
	public PushBadgeFormatException(String message){
		super(message);
	}
	
	public PushBadgeFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushBadgeFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushBadgeFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
