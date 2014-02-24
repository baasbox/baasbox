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
	
	public TwitterLoginService(String appcode) {
		super("twitter",appcode);
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

	/**
	 * {"time_zone":{"name":"Madrid","utc_offset":7200,"tzinfo_name":"Europe\/Madrid"},
	 *  "protected":false,
	 *  "screen_name":"swampie",
	 *  "always_use_https":true,
	 *  "use_cookie_personalization":true,
	 *  "sleep_time":{"enabled":false,"end_time":null,"start_time":null},
	 *  "geo_enabled":true,
	 *  "language":"en","discoverable_by_email":false,"discoverable_by_mobile_phone":false,"display_sensitive_media":false,
	 *  "trend_location":[{"name":"Madrid","countryCode":"ES","url":"http:\/\/where.yahooapis.com\/v1\/place\/766273","woeid":766273,"placeType":{"name":"Town","code":7},"parentid":23424950,"country":"Spain"}]}
	 */
	@Override
	public UserInfo extractUserInfo(Response r) {
		UserInfo i =  new UserInfo();
		JsonNode user = Json.parse(r.getBody());
		i.setUsername(user.get("screen_name").textValue());
		i.addData("location",user.get("time_zone").get("name").asText());
		return i;
	}

}
