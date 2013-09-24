package com.baasbox.service.sociallogin;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.GoogleApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class GooglePlusLoginService extends SocialLoginService{
	
	public GooglePlusLoginService(String appcode){
		super("google",appcode);
	}

	
	@Override
	public String getPrefix() {
		return "gp_";
	}


	@Override
	public Class<? extends Api> provider() {
		return GoogleApi.class;
	}

	@Override
	public Boolean needToken() {
		return false;
	}

	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		StringBuffer url = new StringBuffer(userInfoUrl());
		url.append("?access_token=").append(accessToken.getToken());
		
		return new OAuthRequest(Verb.GET, url.toString());
	}
	
	@Override
	public String userInfoUrl() {
		
		return "https://www.googleapis.com/oauth2/v1/userinfo";
	}

	@Override
	public String getVerifierFromRequest(Request r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Token getAccessTokenFromRequest(Request r, Session s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo i = new UserInfo();
		JsonNode ji = Json.parse(r.getBody());
		String name = ji.get("name").getTextValue();
		
		String username = StringUtils.deleteWhitespace(name.toLowerCase());
		i.addData("email", ji.get("email").asText());
		i.addData("personalUrl", ji.get("link").asText());
		i.setId(ji.get("id").getTextValue());
		i.addData("avatarUrl", ji.get("picture").asText());
		i.addData("name",ji.get("name").getTextValue());
		i.setFrom("google");
		i.setUsername(username);
		return i;
	}

	
	
}
