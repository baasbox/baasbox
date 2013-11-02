package com.baasbox.dao.exception;

public class UpdateOldVersionException extends Exception {
	private int version1;
	private int version2;

	public UpdateOldVersionException() {
		// TODO Auto-generated constructor stub
	}

	public UpdateOldVersionException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public UpdateOldVersionException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public UpdateOldVersionException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public UpdateOldVersionException(String message, int version,
			int version2) {
		super(message);
		this.version1=version;
		this.version2=version2;
	}

	public int getVersion1() {
		return version1;
	}

	public int getVersion2() {
		return version2;
	}
	
	

}
