package com.baasbox.exception;

public class UnableToImportDbException extends Exception {
	
	
	private static final long serialVersionUID = 5409549692932520619L;

	public UnableToImportDbException(Throwable parent){
		super("Unable to import db exception",parent);
	}
}
