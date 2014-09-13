package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushCollapseKeyFormatException extends BaasBoxPushException {
	public PushCollapseKeyFormatException(String message){
		super(message);
	}
	
	public PushCollapseKeyFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushCollapseKeyFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushCollapseKeyFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
