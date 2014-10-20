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

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import play.Logger;
import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

import com.baasbox.configuration.SocialLoginConfiguration;

public class FacebookLoginService extends SocialLoginService{

	public static final String PREFIX = "fb_";
	public static final String SOCIAL = "facebook";
	
	public FacebookLoginService(String appcode) {
		super(SOCIAL,appcode);
	}

	
	
	@Override
	public String getPrefix() {
		return PREFIX;
	}



	@Override
	public Class<? extends Api> provider() {
		return FacebookApi.class;
	}

	@Override
	public Boolean needToken() {
		return false;
	}

	@Override
	public String userInfoUrl() {
		return "https://graph.facebook.com/me";
	}

	
	
	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		return new OAuthRequest(Verb.GET, userInfoUrl());
	}

	@Override
	public String getVerifierFromRequest(Request r) {
		return r.getQueryString("code");
	}

	@Override
	public Token getAccessTokenFromRequest(Request r,Session s) {
		return null;
	}

	
	@Override
	public UserInfo extractUserInfo(Response r) throws BaasBoxFacebookException {
		if (Logger.isDebugEnabled()) Logger.debug("FacebookLoginService.extractUserInfo: " + r.getCode() + ": " + r.getBody());
		UserInfo ui = new UserInfo();
		JsonNode user = Json.parse(r.getBody());
		if (user.has("error")){
			throw new BaasBoxFacebookException(user.get("error"));
		}
		ui.setId(user.get("id").textValue());
		if(user.get("username")!=null){
			ui.setUsername(user.get("username").textValue());
		}
		if(user.get("email")!=null){
			ui.addData("email",user.get("email").textValue());
		}
		if(user.get("gender")!=null){
			ui.addData("gender",user.get("gender").textValue());
		}
		if(user.get("link")!=null){
			ui.addData("personalUrl",user.get("link").textValue());
		}
		if(user.get("name")!=null){
			
			ui.addData("name",user.get("name").textValue());
			if(ui.getUsername()==null){
				ui.setUsername(StringUtils.deleteWhitespace(user.get("name").textValue()));
			}
		}
		ui.setFrom(SOCIAL);
		return ui;
	}



	/**
	 * The validation URL that 
	 */
	@Override
	public String getValidationURL(String token) {
		String url = "https://graph.facebook.com/debug_token?input_token=%s&access_token=%s%%7C%s";
		String finalUrl = String.format(url,token,SocialLoginConfiguration.FACEBOOK_TOKEN.getValueAsString(),SocialLoginConfiguration.FACEBOOK_SECRET.getValueAsString());
		return finalUrl;
	}



	@Override
	protected boolean validate(Object response) throws BaasBoxSocialTokenValidationException {
		if(response instanceof JsonNode){
			JsonNode jn = (JsonNode)response;
			boolean isValid = jn.get("data").get("is_valid").asBoolean();
			if(!isValid){
				throw new BaasBoxSocialTokenValidationException("Provided Facebook auth token is not valid");
			}
			return isValid;
		}else{
			throw new RuntimeException("There was an error invoking validation on facebook token");
		}
	}
	
}
