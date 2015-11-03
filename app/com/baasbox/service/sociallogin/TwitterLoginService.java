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

import org.apache.commons.lang3.StringUtils;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.cache.Cache;
import play.libs.Json;
import play.mvc.Http.Request;

import com.baasbox.BBCache;
import com.baasbox.service.logging.BaasBoxLogger;
import com.fasterxml.jackson.databind.JsonNode;

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
  public Token getAccessTokenFromRequest(Request r) {
	  String token = r.getQueryString("oauth_token");
    String uuid = r.getQueryString("_id");
    if (StringUtils.isEmpty(uuid)) {
      throw new RuntimeException("Unable to retreive the original UUID");
    }

    String cachedToken = (String) Cache.get(BBCache.getTwitterKey() + uuid);
    if (StringUtils.isEmpty(cachedToken)) {
      throw new RuntimeException("Unable to retreive the token from cache");
    }
    if (cachedToken.equals(token)) {
      Cache.remove(BBCache.getTwitterKey() + uuid);
	    return new Token(token,"");
		}else{
			throw new RuntimeException("Unable to retrieve token and secret from session");
		}


		
	}


	

	@Override
  public boolean validationRequest(String token) throws BaasBoxSocialTokenValidationException {
    return true;
  }

  @Override
  protected String saveToken(String k, Token t) {
    BBCache.setTwitterToken(k, t.getToken());
    return k;
  }

  @Override
	public UserInfo extractUserInfo(Response r) {
    BaasBoxLogger.info("Getting twitter user info" + r.getBody());

		UserInfo i =  new UserInfo();
		JsonNode user = Json.parse(r.getBody());
    String screenName = user.get("screen_name").textValue();
    i.setUsername(screenName);
    i.setId(screenName);
		i.addData("location",user.get("time_zone").get("name").asText());
		i.setFrom(SOCIAL);
		return i;
	}

	@Override
	protected String getValidationURL(String token) {
    return "";
	}

	@Override
	protected boolean validate(Object response) {
		// TODO Auto-generated method stub
    return true;
	}
	
	

}
