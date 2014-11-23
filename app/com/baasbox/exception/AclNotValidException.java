package com.baasbox.exception;


public class AclNotValidException extends BaasBoxException {

	public static enum Type{
		ACL_NOT_OBJECT,ACL_KEY_NOT_VALID, ACL_USER_OR_ROLE_KEY_UNKNOWN, ACL_USER_DOES_NOT_EXIST, ACL_ROLE_DOES_NOT_EXIST, JSON_VALUE_MUST_BE_ARRAY
	}

	private Type type;
	
	public AclNotValidException(Type type) {
		this.type=type;
	}
	
	public AclNotValidException(Type type,String arg0) {
		super(arg0);
		this.type=type;
	}

	public AclNotValidException(Type type,Throwable arg0) {
		super(arg0);
		this.type=type;
	}

	public AclNotValidException(Type type,String arg0, Throwable arg1) {
		super(arg0, arg1);
		this.type=type;
	}
	
	public Type getType(){
		return type;
	}

}
