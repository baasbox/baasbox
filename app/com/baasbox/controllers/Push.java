package com.baasbox.controllers;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilter;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.push.providers.PushNotInitializedException;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

@With ({UserCredentialWrapFilter.class,ConnectToDBFilter.class})
public class Push extends Controller {
	 public static Result send(String message, String username) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException {
		Logger.trace("Method Start");
		 
		 
		 PushService ps=new PushService();
		try{
			ps.send(message, username);
		}
		catch (UserNotFoundException e) {
			Logger.error("Username not found " + username, e);
		}
		catch (SqlInjectionException e) {
			return badRequest("the supplied name appears invalid (Sql Injection Attack detected)");
		}
		
		Logger.trace("Method End");
		return ok();
	 }
}
