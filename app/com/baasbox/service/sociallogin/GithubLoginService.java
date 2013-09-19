package com.baasbox.service.sociallogin;

import org.codehaus.jackson.JsonNode;
import org.scribe.builder.api.Api;
import org.scribe.model.Response;
import org.scribe.model.Token;

import play.libs.Json;
import play.mvc.Http.Request;
import play.mvc.Http.Session;

public class GithubLoginService extends SocialLoginService {

	public GithubLoginService(String appcode) {
		super("github",appcode);
	}

	@Override
	public Class<? extends Api> provider() {
		return GithubApi.class;
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

	/**
	 * {"login":"swampie",
	 *  "id":657223,
	 *  "avatar_url":"https://2.gravatar.com/avatar/837b6997d042e92f884abd5b374a9e25?d=https%3A%2F%2Fidenticons.github.com%2Fdcab7898d12eaa721957983a8e84e05f.png",
	 *  "gravatar_id":"837b6997d042e92f884abd5b374a9e25",
	 *  "url":"https://api.github.com/users/swampie",
	 *  "html_url":"https://github.com/swampie",
	 *  "followers_url":"https://api.github.com/users/swampie/followers",
	 *  "following_url":"https://api.github.com/users/swampie/following{/other_user}",
	 *  "gists_url":"https://api.github.com/users/swampie/gists{/gist_id}",
	 *  "starred_url":"https://api.github.com/users/swampie/starred{/owner}{/repo}",
	 *  "subscriptions_url":"https://api.github.com/users/swampie/subscriptions",
	 *  "organizations_url":"https://api.github.com/users/swampie/orgs",
	 *  "repos_url":"https://api.github.com/users/swampie/repos",
	 *  "events_url":"https://api.github.com/users/swampie/events{/privacy}",
	 *  "received_events_url":"https://api.github.com/users/swampie/received_events",
	 *  "type":"User",
	 *  "name":"Matteo FIandesio","company":null,"blog":null,"location":"Madrid","email":null,"hireable":false,"bio":null,"public_repos":20,"followers":5,"following":4,"created_at":"2011-03-08T08:01:13Z","updated_at":"2013-09-17T12:08:03Z","public_gists":0}
	 */
	@Override
	public UserInfo extractUserInfo(Response r) {
		JsonNode user = Json.parse(r.getBody());
		UserInfo ui = new UserInfo();
		ui.setUsername(user.get("login").getTextValue());
		ui.addData("avatar", user.get("avatar_url").getTextValue());
		ui.addData("personal_url", user.get("html_url").getTextValue());
		ui.addData("name", user.get("name").getTextValue());
		ui.addData("location", user.get("location").getTextValue());
		return ui;
		
	}

}
