package com.baasbox.controllers;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

public class Generic extends Controller{

	public static Result getOptions(String parameter){
		response().setHeader("Allow", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Origin", "*");
		if (request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS")!=null)
			response().setHeader("Access-Control-Allow-Headers",request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS"));
		return ok();
	}
	
	@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result refreshSessionToken(){
		return ok(); 
	}
	
}
