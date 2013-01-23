package com.baasbox.controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class Generic extends Controller{

	public static Result getOptions(String parameter){
		response().setHeader("Allow", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
		response().setHeader("Access-Control-Allow-Origin", "*");
		if (request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS")!=null)
			response().setHeader("Access-Control-Allow-Headers",request().getHeader("ACCESS-CONTROL-REQUEST-HEADERS"));
		return ok();
	}
	
}
