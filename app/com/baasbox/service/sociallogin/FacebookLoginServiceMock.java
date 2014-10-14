package com.baasbox.service.sociallogin;

import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.resources.selectors.Date;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class FacebookLoginServiceMock extends SocialLoginService {

	public static final String PREFIX = "fb_";
	public static final String SOCIAL = "facebook";
	
	public FacebookLoginServiceMock(String appcode) {
		super(SOCIAL,appcode);
	}

	@Override
	public Class<? extends Api> provider() {
		return MockApi.class;
	}

	@Override
	public Boolean needToken() {
		return false;
	}

	@Override
	public String userInfoUrl() {
		return "http://www.example.com";
	}

	@Override
	public String getVerifierFromRequest(Request r) {
		return "mock_verifier";
	}

	@Override
	public Token getAccessTokenFromRequest(Request r, Session s) {
		return null;
	}

	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		return new OAuthRequest(Verb.GET, userInfoUrl());
	}

	@Override
	public UserInfo extractUserInfo(Response r) throws BaasBoxSocialException {
		if (Logger.isDebugEnabled()) Logger.debug("FacebookLoginServiceMock.extractUserInfo: " + r.getCode() + ": " + r.getBody());
		UserInfo ui = new UserInfo();
		ui.setId("mockid_" + UUID.randomUUID());
		ui.setUsername("mockusername_" + UUID.randomUUID());
		ui.addData("email",ui.getUsername() + "@example.com");
		ui.addData("gender","M");
		ui.addData("personalUrl","http://www.example.com/" + ui.getUsername());		
		ui.addData("name","Mario Rossi Mock " + System.currentTimeMillis());
		ui.setFrom(SOCIAL + "_mock");
		return ui;
	}

	@Override
	public String getPrefix() {
		return PREFIX + "_mock_";
	}

	@Override
	protected String getValidationURL(String token) {
		return "http://www.example.com";
	}

	@Override
	protected boolean validate(Object response) {
		return true;
	}

	@Override
	public boolean validationRequest(String token) throws BaasBoxSocialTokenValidationException{
		return true;
	}
	
	@Override
	public UserInfo getUserInfo(Token accessToken) throws BaasBoxSocialException{

		OAuthRequest request = buildOauthRequestForUserInfo(accessToken);

		this.service.signRequest(accessToken, request);
		Response response = request.send();
		return extractUserInfo(response);
	}
}
