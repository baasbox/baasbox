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
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.baasbox.configuration.SocialLoginConfiguration;

import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class GithubLoginService extends SocialLoginService {

	public static String PREFIX = "gh_";
	public GithubLoginService(String appcode) {
		super("github",appcode);
	}

	
	
	@Override
	public String getPrefix() {
		return PREFIX;
	}



	@Override
	public Class<? extends Api> provider() {
		return GithubApi.class;
	}

	@Override
	protected OAuthRequest buildOauthRequestForUserInfo(Token accessToken) {
		return new OAuthRequest(Verb.GET, userInfoUrl());
	}
	@Override
	public Boolean needToken() {
		return false;
	}

	@Override
	public String userInfoUrl() {
		return "https://api.github.com/user";
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
	public UserInfo extractUserInfo(Response r) {
		JsonNode user = Json.parse(r.getBody());
		UserInfo ui = new UserInfo();
		ui.setId(user.get("id").textValue());
		ui.setUsername(user.get("login").textValue());
		if(user.get("avatar_url")!=null){
			ui.addData("avatar", user.get("avatar_url").textValue());
		}
		if(user.get("html_url")!=null){
			ui.addData("personal_url", user.get("html_url").textValue());
		}
		if(user.get("name")!=null){
			ui.addData("name", user.get("name").textValue());
		}
		if(user.get("location")!=null){
			ui.addData("location", user.get("location").textValue());
		}
		return ui;
		
	}


	@Override
	protected String getValidationURL(String token) {
		return String.format("https://api.github.com/");
	}



	@Override
	protected boolean validate(Object response) {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
