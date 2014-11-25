package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushContentAvailableFormatException extends BaasBoxPushException {
	public PushContentAvailableFormatException(String message){
		super(message);
	}
	
	public PushContentAvailableFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushContentAvailableFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushContentAvailableFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
