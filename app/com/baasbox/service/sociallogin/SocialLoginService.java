package com.baasbox.service.sociallogin;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

import com.baasbox.configuration.SocialLoginConfiguration;

public abstract class SocialLoginService {

	
	
	protected OAuthService service;
	
	
	public OAuthService getService() {
		return service;
	}

	protected Tokens token;
	protected String socialNetwork;
	protected String appcode;
	public abstract Class<? extends Api> provider();
	public abstract Boolean needToken();
	public abstract String userInfoUrl();
	
	public abstract String getVerifierFromRequest(Request r);
	public abstract Token getAccessTokenFromRequest(Request r,Session s);
	
	
	public void build(){
		this.service = new ServiceBuilder().
				provider(provider())
				.apiKey(this.token.getToken())
				.apiSecret(this.token.getSecret())
				.callback("http://omg.mfiandesio.com:9000/login/"+socialNetwork+"/callback")
				.build();
	}
	
	public SocialLoginService(String socialNetwork,String appcode){
		this.socialNetwork = socialNetwork;
		this.appcode = appcode;
		this.token = getTokens();
		build();
	}
	
	public String getAuthorizationURL(Session s){
		Token t = null;
		if(this.needToken()){
			t = this.service.getRequestToken();
			if(this.socialNetwork.equals("twitter")){
				s.put("twitter.token",t.getToken());
				s.put("twitter.secret",t.getSecret());
			}
		}
		return this.service.getAuthorizationUrl(t);
	}
	
	public Token requestAccessToken(Request r,Session s){
		Token t = getAccessTokenFromRequest(r,s);
		Verifier v = new Verifier(getVerifierFromRequest(r));
		return this.service.getAccessToken(t, v);
	}
	
	public UserInfo getUserInfo(Token accessToken){
		OAuthRequest request = new OAuthRequest(Verb.GET, userInfoUrl());
		this.service.signRequest(accessToken, request);
		Response response = request.send();
		return extractUserInfo(response);
	}
	
	public boolean isClientEnabled(){
		String keyFormat = socialNetwork.toUpperCase()+"_ENABLED";
		Boolean enabled = SocialLoginConfiguration.valueOf(keyFormat).getValueAsBoolean();
		return enabled == null ? false : enabled;
	}

	public  Tokens getTokens(){
		String keyFormat = socialNetwork.toUpperCase()+"_TOKEN";
		String token = (String)Cache.get(keyFormat);
		if(token ==null){
			token = SocialLoginConfiguration.valueOf(keyFormat).getValueAsString();
			Cache.set(keyFormat,token,0);
		}
		keyFormat =  socialNetwork.toUpperCase()+"_SECRET";
		String secret =  (String)Cache.get(keyFormat);;
		if(secret ==null){
			secret = SocialLoginConfiguration.valueOf(keyFormat).getValueAsString();
			Cache.set(keyFormat,secret,0);
		}
		return new Tokens(token,secret);
	}

	public static class Tokens{
		private String token;
		private String secret;

		public Tokens(String token,String secret){
			assert(!(StringUtils.isEmpty(token) || StringUtils.isEmpty(secret)));

			this.token = token;
			this.secret = secret;
		}

		public String getToken() {
			return token;
		}

		public String getSecret() {
			return secret;
		}

		public String toString(){
			return "Tokens:{token:"+this.token+",secret:"+this.secret+"}";
		}

	}

	public abstract  UserInfo extractUserInfo(Response r);
	
	public static SocialLoginService by(String socialNetwork,String appcode) {
		if(socialNetwork.equals("facebook")){
			return new FacebookLoginService(appcode);
		}else if(socialNetwork.equals("twitter")){
			return new TwitterLoginService(appcode);
		}else if(socialNetwork.equals("github")){
			return new GithubLoginService(appcode);
		}
		return null;
	}


	
}
