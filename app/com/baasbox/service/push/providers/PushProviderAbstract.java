package com.baasbox.service.push.providers;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.common.collect.ImmutableMap;

public abstract class PushProviderAbstract implements IPushServer {

	
	
	public abstract void setConfiguration(ImmutableMap<ConfigurationKeys, String> configuration);

	public abstract boolean send(String message, List<String> deviceid, JsonNode bodyJson)
			throws PushNotInitializedException, UnknownHostException,
			InvalidRequestException, IOException, Exception;
	

}
