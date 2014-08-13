package com.baasbox.exception;

public abstract class BaasBoxPushException extends Exception {
	public BaasBoxPushException(String message){
		super(message);
	}
	
	public BaasBoxPushException() {
		super();
		// TODO Auto-generated constructor stub
	}



	public BaasBoxPushException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxPushException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
