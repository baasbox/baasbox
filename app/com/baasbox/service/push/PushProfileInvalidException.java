package com.baasbox.service.push;

import com.baasbox.exception.BaasBoxPushException;

public class PushProfileInvalidException extends BaasBoxPushException {
	public PushProfileInvalidException(String message){
		super(message);
	}
	
	public PushProfileInvalidException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushProfileInvalidException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushProfileInvalidException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
