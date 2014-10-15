/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.sociallogin;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import play.Logger;
import play.cache.Cache;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Application;
import com.baasbox.configuration.SocialLoginConfiguration;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

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
		//since this method can be called by the /callback endpoint that does not open a DB connection, we need to manage it here
		try{
			DbHelper.getOrOpenConnectionWIthHTTPUsername();		
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
		}catch(InvalidAppCodeException iace){
			throw new RuntimeException(iace);
		}

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

	public UserInfo getUserInfo(Token accessToken) throws BaasBoxSocialException{

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
		//since this method can be called by the /callback endpoint that does not open a DB connection, we need to manage it here
		if (BBConfiguration.getSocialMock())  return new Tokens("fake_token","fake_secret");
		ODatabaseRecordTx db=null;
		try {
			db = DbHelper.getOrOpenConnection(BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxUsername(), BBConfiguration.getBaasBoxPassword());		
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
		} catch (InvalidAppCodeException e) {
			//a very strange thing happened here!
			throw new RuntimeException(e);
		}finally{
			if (db!=null && !db.isClosed()) db.close();
		}
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

	public abstract  UserInfo extractUserInfo(Response r) throws BaasBoxSocialException;

	public static SocialLoginService by(String socialNetwork,String appcode) {
		if (BBConfiguration.getSocialMock()) return new SocialLoginServiceMock(socialNetwork,appcode);

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
	protected abstract String getValidationURL(String token);

	public boolean validationRequest(String token) throws BaasBoxSocialTokenValidationException{
		String url = getValidationURL(token);
		HttpClient client = new DefaultHttpClient();
		HttpGet method = new HttpGet(url);

		BasicResponseHandler brh = new BasicResponseHandler();

		String body;
		try {
			body = client.execute(method,brh);

			if(StringUtils.isEmpty(body)){
				return false;
			}else{
				ObjectMapper mapper = new ObjectMapper();
				JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
				JsonParser jp = factory.createJsonParser(body);
				JsonNode jn = mapper.readTree(jp);
				return validate(jn);
			}
		} catch (IOException e) {
			throw new BaasBoxSocialTokenValidationException("There was an error in the token validation process:"+e.getMessage());
		}

	}

	protected abstract boolean validate(Object response);


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
