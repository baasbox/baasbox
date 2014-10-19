package com.baasbox.service.sociallogin;

import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.Logger;
import play.Play;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class SocialLoginServiceMock extends SocialLoginService {
	
	private static String port = (Play.isTest())?System.getProperty("testserver.port", "3333"):System.getProperty("http.port", "9000");
	private static String mockUrl = "http://localhost"+port;
	
	public   String PREFIX = "";
	public   String SOCIAL = "";
	
	private String secret="";
	private String token="";
	
	public SocialLoginServiceMock(String socialNetworkName, String appcode) {
		super(socialNetworkName,appcode);
		this.SOCIAL=socialNetworkName;
		if(socialNetwork.equals("facebook")){
			this.PREFIX=FacebookLoginService.PREFIX;
		}else if(socialNetwork.equals("github")){
			this.PREFIX=GithubLoginService.PREFIX;
		}else if(socialNetwork.equals("google")){
			this.PREFIX=GooglePlusLoginService.PREFIX;
		}
		this.PREFIX=this.PREFIX + "_mock_";
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
		return mockUrl;
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
		ui.setId("mockid_" + this.token);
		ui.setUsername("mockusername_" + this.token);
		ui.addData("email",ui.getUsername() + "@example.com");
		ui.addData("gender","male");
		ui.addData("personalUrl","http://www.example.com/" + ui.getUsername());		
		ui.addData("name","John Doe Mock " + System.currentTimeMillis());
		ui.setFrom(SOCIAL + "_mock");
		return ui;
	}

	@Override
	public String getPrefix() {
		return PREFIX + "_mock_";
	}

	@Override
	protected String getValidationURL(String token) {
		return mockUrl;
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
		this.secret=accessToken.getSecret();
		this.token=accessToken.getToken();
		
		OAuthRequest request = buildOauthRequestForUserInfo(accessToken);

		this.service.signRequest(accessToken, request);
		Response response = null;
		try {
			response = request.send();
		}catch(Exception e){
			//swallow
		}
		return extractUserInfo(response);
	}
}
