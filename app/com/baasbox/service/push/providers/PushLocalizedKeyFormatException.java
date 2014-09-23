package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushLocalizedKeyFormatException extends BaasBoxPushException {
	public PushLocalizedKeyFormatException(String message){
		super(message);
	}
	
	public PushLocalizedKeyFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushLocalizedKeyFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushLocalizedKeyFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
