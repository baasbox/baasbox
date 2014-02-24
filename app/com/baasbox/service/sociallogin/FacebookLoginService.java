package com.baasbox.service.sociallogin;

import com.fasterxml.jackson.databind.JsonNode;
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

	public static String PREFIX = "fb_";
	
	public FacebookLoginService(String appcode) {
		super("facebook",appcode);
	}

	
	
	@Override
	public String getPrefix() {
		return PREFIX;
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

	
	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo ui = new UserInfo();
		JsonNode user = Json.parse(r.getBody());
		ui.setUsername(user.get("username").textValue());
		ui.setId(user.get("id").textValue());
		if(user.get("email")!=null){
			ui.addData("email",user.get("email").textValue());
		}
		if(user.get("gender")!=null){
			ui.addData("gender",user.get("gender").textValue());
		}
		if(user.get("link")!=null){
			ui.addData("personalUrl",user.get("link").textValue());
		}
		if(user.get("name")!=null){
			ui.addData("name",user.get("name").textValue());
		}
		return ui;
	}

	
	
	
}
