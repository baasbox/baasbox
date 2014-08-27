package com.baasbox.service.push;

import com.baasbox.exception.BaasBoxPushException;

public class PushInvalidApiKeyException extends BaasBoxPushException {
	public PushInvalidApiKeyException(String message){
		super(message);
	}
	
	public PushInvalidApiKeyException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushInvalidApiKeyException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushInvalidApiKeyException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
