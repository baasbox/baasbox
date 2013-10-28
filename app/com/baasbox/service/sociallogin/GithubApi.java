package com.baasbox.service.sociallogin;


import org.scribe.builder.api.DefaultApi20;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Verb;
import org.scribe.utils.OAuthEncoder;

import com.baasbox.service.sociallogin.extractors.GithubTokenExtractor;

public class GithubApi extends DefaultApi20 
{

	private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize?client_id=%s";
	private static final String SCOPED_AUTHORIZE_URL = AUTHORIZE_URL+ "&scope=%s";

	@Override
	public String getAccessTokenEndpoint() 
	{
		return "https://github.com/login/oauth/access_token";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig config) 
	{
		// Append scope if present
		if (config.hasScope()) 
		{
			return String.format(SCOPED_AUTHORIZE_URL, config.getApiKey(),
									OAuthEncoder.encode(config.getCallback()),
									OAuthEncoder.encode(config.getScope()));
		} 
		else 
		{
			return String.format(AUTHORIZE_URL, config.getApiKey(),
									OAuthEncoder.encode(config.getCallback()));
		}
	}

	@Override
	public Verb getAccessTokenVerb()
	{
		return Verb.POST;
	}

	@Override
	public GithubTokenExtractor getAccessTokenExtractor() 
	{
		return new GithubTokenExtractor();
	}
}
