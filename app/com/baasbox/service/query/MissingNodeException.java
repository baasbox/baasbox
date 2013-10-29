package com.baasbox.service.query;

public class MissingNodeException extends Exception{
	
	
	public MissingNodeException(String path){
		super(String.format("%s is not a valid path",path));
	}
	
}
