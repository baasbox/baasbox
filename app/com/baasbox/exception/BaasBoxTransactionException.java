package com.baasbox.exception;

public abstract class BaasBoxTransactionException extends RuntimeException{

	public BaasBoxTransactionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxTransactionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxTransactionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxTransactionException() {
		// TODO Auto-generated constructor stub
	}

}
