package com.baasbox.exception;


public class AclNotValidException extends BaasBoxException {

	int result;
	public AclNotValidException() {}

	public AclNotValidException(int result,String arg0) {
		super(arg0);
		this.result=result;
	}

	public AclNotValidException(int result,Throwable arg0) {
		super(arg0);
		this.result=result;
	}

	public AclNotValidException(int result,String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.result=result;
	}
	
	public int getResultCode(){
		return result;
	}

}
