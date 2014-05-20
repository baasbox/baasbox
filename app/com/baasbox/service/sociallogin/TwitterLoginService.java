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

import com.fasterxml.jackson.databind.JsonNode;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.Logger;
import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class TwitterLoginService extends SocialLoginService {

	public static String PREFIX = "tw_";
	public static String SOCIAL = "twitter";
	
	public TwitterLoginService(String appcode) {
		super(SOCIAL,appcode);
	}

	@Override
	public Class<? extends Api> provider() {
		return TwitterApi.class;
	}

	@Override
	public Boolean needToken() {
		return true;
	}
	
	

	@Override
	public String getPrefix() {
		return PREFIX;
	}

	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		return new OAuthRequest(Verb.GET, userInfoUrl());
	}
	
	@Override
	public String userInfoUrl() {
		return "https://api.twitter.com/1.1/account/settings.json";
	}

	@Override
	public String getVerifierFromRequest(Request r) {
		return r.getQueryString("oauth_verifier");
	}

	@Override
	public Token getAccessTokenFromRequest(Request r,Session s) {
		if (Logger.isDebugEnabled()) Logger.debug(Json.stringify(Json.toJson(s.keySet())));
		if(s.get("twitter.token")!=null && s.get("twitter.secret")!=null){
			String token = s.get("twitter.token");
			String secret = s.get("twitter.secret");
			Token t = new Token(token,secret);
			s.remove("twitter.token");
			s.remove("twitter.secret");
			return t;
		}else{
			throw new RuntimeException("Unable to retrieve token and secret from session");
		}


		
	}

	
	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo i =  new UserInfo();
		JsonNode user = Json.parse(r.getBody());
		i.setUsername(user.get("screen_name").textValue());
		i.addData("location",user.get("time_zone").get("name").asText());
		i.setFrom(SOCIAL);
		return i;
	}

	@Override
	protected String getValidationURL(String token) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean validate(Object response) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
