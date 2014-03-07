package com.baasbox.controllers;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.RootCredentialWrapFilter;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.user.UserService;


public class Root extends Controller {

	@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result resetAdminPassword(){
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (Logger.isDebugEnabled()) Logger.debug("resetAdminPassword bodyJson: " + bodyJson);
		//check and validate input
		if (bodyJson==null) return badRequest("The body payload cannot be empty.");		
		if (!bodyJson.has("password"))	return badRequest("The 'password' field is missing into the body");
		JsonNode passwordNode=bodyJson.findValue("password");
		
		if (passwordNode==null) return badRequest("The body payload doesn't contain password field");
		String password=passwordNode.asText();	 
		try{
			UserService.changePassword("admin", password);
		} catch (SqlInjectionException e) {
			return badRequest("The password is not valid");
		} catch (UserNotFoundException e) {
			Logger.error("User 'admin' not found!");
		    return internalServerError("User 'admin' not found!");
		}
		return ok("Admin password reset");
	}
	
}
