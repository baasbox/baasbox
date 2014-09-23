package com.baasbox.service.push;

import com.baasbox.exception.BaasBoxPushException;

public class PushProfileDisabledException extends BaasBoxPushException {
	public PushProfileDisabledException(String message){
		super(message);
	}
	
	public PushProfileDisabledException() {
		super();
		// TODO Auto-generated constructor stub
	}



	public PushProfileDisabledException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushProfileDisabledException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
