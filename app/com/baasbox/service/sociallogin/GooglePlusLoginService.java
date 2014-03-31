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
	public static String SOCIAL = "google";
	public static String PREFIX = "gp_";
	
	public GooglePlusLoginService(String appcode){
		super(SOCIAL,appcode);
	}

	
	@Override
	public String getPrefix() {
		return PREFIX;
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
		i.setId(ji.get("id").getTextValue());
		if(ji.get("email")!=null){
			i.addData("email", ji.get("email").asText());
		}
		if(ji.get("link")!=null){
			i.addData("personal_url", ji.get("link").asText());
		}
		if(ji.get("picture")!=null){
			i.addData("avatarUrl", ji.get("picture").asText());
		}
		if(ji.get("email")!=null){
			i.addData("name",ji.get("name").getTextValue());
			String name = ji.get("name").getTextValue();
			String username = StringUtils.deleteWhitespace(name.toLowerCase());
			i.setUsername(username);
		}
		i.setFrom(SOCIAL);
		return i;
	}


	@Override
	protected String getValidationURL(String token) {
		String template = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=%s";
		return String.format(template,token);
	}


	@Override
	protected boolean validate(Object response) throws BaasBoxSocialTokenValidationException {
		if(response instanceof JsonNode){
			JsonNode jn = (JsonNode)response;
			return StringUtils.isNotEmpty(jn.get("user_id").getTextValue());
		}else{
			throw new BaasBoxSocialTokenValidationException();
		}
	}
	
	

	
	
}
