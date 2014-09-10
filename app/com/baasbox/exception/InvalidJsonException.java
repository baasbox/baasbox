package com.baasbox.exception;

import com.orientechnologies.orient.core.exception.OSerializationException;

public class InvalidJsonException extends Exception {

	public InvalidJsonException() {
		// TODO Auto-generated constructor stub
	}

	public InvalidJsonException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public InvalidJsonException(OSerializationException cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public InvalidJsonException(String message, OSerializationException cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
