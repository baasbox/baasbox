package com.baasbox.exception;

public class UnableToExportDbException extends Exception {
	
	
	private static final long serialVersionUID = 5409549692932520619L;

	public UnableToExportDbException(Throwable parent){
		super("Unable to export db exception",parent);
	}
}
