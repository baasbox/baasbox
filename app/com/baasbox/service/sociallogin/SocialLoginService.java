package com.baasbox.service.sociallogin;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.Logger;
import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

import com.baasbox.configuration.Application;
import com.baasbox.configuration.SocialLoginConfiguration;

public abstract class SocialLoginService {

	private static final String PROTOCOL = "http://";
	private static final String SECURE_PROTOCOL = "https://";
	private static final String DEFAULT_HOST = "localhost";
	private static final String DEFAULT_PORT = "9000";
	
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
		StringBuilder serverUrl = new StringBuilder();
		Boolean isSSL = (Boolean)Application.NETWORK_HTTP_SSL.getValueAsBoolean();
		if(isSSL){
			serverUrl.append(SECURE_PROTOCOL);
		}else{
			serverUrl.append(PROTOCOL);
		}
		String serverName = Application.NETWORK_HTTP_URL.getValueAsString();
		serverUrl.append(serverName!=null?serverName:DEFAULT_HOST);
		String serverPort = Application.NETWORK_HTTP_PORT.getValueAsString();
		serverUrl.append(serverPort!=null?":"+serverPort:":"+DEFAULT_PORT);
		this.service = new ServiceBuilder().
				provider(provider())
				.apiKey(this.token.getToken())
				.apiSecret(this.token.getSecret())
				.callback(serverUrl.toString()+"/login/"+socialNetwork+"/callback")
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
				if (Logger.isDebugEnabled()) Logger.debug("setting token");
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
		
		OAuthRequest request = buildOauthRequestForUserInfo(accessToken);
		
		this.service.signRequest(accessToken, request);
		Response response = request.send();
		return extractUserInfo(response);
	}
	
	protected abstract OAuthRequest buildOauthRequestForUserInfo(Token accessToken);
	public boolean isClientEnabled(){
		String keyFormat = socialNetwork.toUpperCase()+"_ENABLED";
		Boolean enabled = SocialLoginConfiguration.valueOf(keyFormat).getValueAsBoolean();
		return enabled == null ? false : enabled;
	}

	public  Tokens getTokens() throws UnsupportedSocialNetworkException{
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
		if(secret==null || token == null){
			throw new UnsupportedSocialNetworkException("Social login for "+socialNetwork+" is not enabled.Please add app token and secret to configuration");
		}
		return new Tokens(token,secret);
	}

	public static class Tokens implements Serializable{
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
		}else if(socialNetwork.equals("github")){
			return new GithubLoginService(appcode);
		}else if(socialNetwork.equals("google")){
			return new GooglePlusLoginService(appcode);
		}
		return null;
	}
	public abstract String getPrefix();
	
	public  String getPrefixByName(String from) {
		if(from.equals("facebook")){
			return FacebookLoginService.PREFIX;
		}else if(socialNetwork.equals("github")){
			return GithubLoginService.PREFIX;
		}else if(socialNetwork.equals("google")){
			return GooglePlusLoginService.PREFIX;
		}
		throw new InvalidSocialNetworkNameException(from);
	}


	
}
