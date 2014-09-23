package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushLocalizedArgumentsFormatException extends BaasBoxPushException {
	public PushLocalizedArgumentsFormatException(String message){
		super(message);
	}
	
	public PushLocalizedArgumentsFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushLocalizedArgumentsFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushLocalizedArgumentsFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
