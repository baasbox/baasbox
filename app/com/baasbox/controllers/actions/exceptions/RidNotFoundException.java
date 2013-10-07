package com.baasbox.controllers.actions.exceptions;

public class RidNotFoundException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public RidNotFoundException (String id) {
		super("UUID " + id + " not found");
	}
	
}
