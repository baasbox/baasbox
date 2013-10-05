package com.baasbox.service.push.providers;





import java.io.IOException;
import java.net.UnknownHostException;

import com.google.android.gcm.server.InvalidRequestException;
import com.google.common.collect.ImmutableMap;


public interface IPushServer{
	public void setConfiguration(ImmutableMap<Factory.ConfigurationKeys,String> configuration);
	public void send(String message, String deviceid) throws PushNotInitializedException, UnknownHostException, InvalidRequestException, IOException;
}
