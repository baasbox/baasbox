package com.baasbox.service.sociallogin;

import org.codehaus.jackson.JsonNode;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class FacebookLoginService extends SocialLoginService{

	public FacebookLoginService(String appcode) {
		super("facebook",appcode);
	}

	@Override
	public Class<? extends Api> provider() {
		return FacebookApi.class;
	}

	@Override
	public Boolean needToken() {
		return false;
	}

	@Override
	public String userInfoUrl() {
		return "https://graph.facebook.com/me";
	}

	
	
	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		return new OAuthRequest(Verb.GET, userInfoUrl());
	}

	@Override
	public String getVerifierFromRequest(Request r) {
		return r.getQueryString("code");
	}

	@Override
	public Token getAccessTokenFromRequest(Request r,Session s) {
		return null;
	}

	/**
	 * {"id":"100000161140648",
	 * "name":"Matt Fiand",
	 * "first_name":"Matt",
	 * "last_name":"Fiand",
	 * "link":"https:\/\/www.facebook.com\/matt.fiand",
	 * "username":"matt.fiand",
	 * "birthday":"01\/23\/1981",
	 * "gender":"male",
	 * "email":"matteo.fiandesio\u004011870.com",
	 * "timezone":2,"locale":"en_US","updated_time":"2012-06-13T13:01:41+0000"}
	 */
	
	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo ui = new UserInfo();
		JsonNode user = Json.parse(r.getBody());
		ui.setUsername(user.get("username").getTextValue());
		ui.setId(user.get("id").getTextValue());
		ui.addData("email",user.get("email").getTextValue());
		ui.addData("gender",user.get("gender").getTextValue());
		ui.addData("personalUrl",user.get("link").getTextValue());
		ui.addData("name",user.get("name").getTextValue());
		return ui;
	}

	
	
	
}
