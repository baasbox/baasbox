package com.baasbox.exception;

@SuppressWarnings("serial")
public class RoleNotFoundException extends Exception {

	private boolean inehrited=false;
	public RoleNotFoundException() {
		// TODO Auto-generated constructor stub
	}

	public RoleNotFoundException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public RoleNotFoundException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public RoleNotFoundException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public boolean isInehrited() {
		return inehrited;
	}

	public void setInehrited(boolean inehrited) {
		this.inehrited = inehrited;
	}

}
