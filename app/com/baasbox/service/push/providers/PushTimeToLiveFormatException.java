package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushTimeToLiveFormatException extends BaasBoxPushException {
	public PushTimeToLiveFormatException(String message){
		super(message);
	}
	
	public PushTimeToLiveFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushTimeToLiveFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushTimeToLiveFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
