package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushCategoryFormatException extends BaasBoxPushException {
	public PushCategoryFormatException(String message){
		super(message);
	}
	
	public PushCategoryFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushCategoryFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushCategoryFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
