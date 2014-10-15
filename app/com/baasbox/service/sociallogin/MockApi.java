package com.baasbox.service.sociallogin;

import org.scribe.builder.api.Api;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class MockApi implements Api {

	 public static OAuthConfig config;

	    public OAuthService createService(OAuthConfig config)    {
	      MockApi.config = config;
	      return new OAuthService() {
			
			@Override
			public void signRequest(Token arg0, OAuthRequest arg1) {
			}
			
			@Override
			public String getVersion() {
				return "MockApi 1.0";
			}
			
			@Override
			public Token getRequestToken() {
				return new Token("mock_token", "mock_secret");
			}
			
			@Override
			public String getAuthorizationUrl(Token arg0) {
				return "http://www.example.com";
			}
			
			@Override
			public Token getAccessToken(Token arg0, Verifier arg1) {
				return new Token("mock_access_token", "mock_access_secret");
			}
		};
	    }
}
