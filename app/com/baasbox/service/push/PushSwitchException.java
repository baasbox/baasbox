package com.baasbox.service.push;

import com.baasbox.exception.BaasBoxPushException;

public class PushSwitchException extends BaasBoxPushException {
	public PushSwitchException(String message){
		super(message);
	}
	
	public PushSwitchException() {
		super();
		// TODO Auto-generated constructor stub
	}



	public PushSwitchException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushSwitchException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
