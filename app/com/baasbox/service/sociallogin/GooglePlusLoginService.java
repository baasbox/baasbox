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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.GoogleApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.libs.Json;
import play.mvc.Http.Request;

import com.baasbox.configuration.SocialLoginConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

public class GooglePlusLoginService extends SocialLoginService{
	public static String SOCIAL = "google";
	public static String PREFIX = "gp_";
	
	public GooglePlusLoginService(String appcode){
		super(SOCIAL,appcode);
	}

	
	@Override
  protected String saveToken(String k, Token t) {
		throw new UnsupportedOperationException();
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
		return null;
	}

	@Override
  public Token getAccessTokenFromRequest(Request r) {
		return null;
	}

	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo i = new UserInfo();
		JsonNode ji = Json.parse(r.getBody());
		i.setId(ji.get("id").textValue());
		if(ji.get("email")!=null){
			i.addData("email", ji.get("email").textValue());
		}
		if(ji.get("link")!=null){
			i.addData("personal_url", ji.get("link").textValue());
		}
		if(ji.get("picture")!=null){
			i.addData("avatarUrl", ji.get("picture").textValue());
		}
		if(ji.get("name")!=null){
			i.addData("name",ji.get("name").textValue());
			String name = ji.get("name").textValue();
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
			String clientId = jn.get("issued_to").textValue();
      boolean isValid = clientId != null
        && (clientId.equals(SocialLoginConfiguration.GOOGLE_TOKEN.getValueAsString()) || isSubset(clientId, SocialLoginConfiguration.GOOGLE_TOKEN.getValueAsString()));
			if(!isValid){
				throw new BaasBoxSocialTokenValidationException("The provided g+ token is not valid");
			}
			return isValid;
		}else{
			throw new RuntimeException("G+ validation token failed");
		}
	}

  private boolean isSubset(String clientId, String parentToken) {
    Pattern p = Pattern.compile("^([0-9]+)(-(.*))?\\.apps.googleusercontent.com$");
    Matcher firstMatcher = p.matcher(clientId);
    Matcher secondMatcher = p.matcher(parentToken);
    String header = header(firstMatcher);
    if (header == null) {
      return false;
    }
    String secondHeader = header(secondMatcher);
    if (secondHeader == null) {
      return false;
    }
    return header.equals(secondHeader);
  }

  private static String header(Matcher m) {
    if (m.matches()) {
      return m.group(1);
    } else {
      return null;
    }
  }


	
	
}
