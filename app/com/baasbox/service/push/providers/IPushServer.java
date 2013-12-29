package com.baasbox.service.push.providers;





import com.google.common.collect.ImmutableMap;


public interface IPushServer{
	public void setConfiguration(ImmutableMap<Factory.ConfigurationKeys,String> configuration);
	public void send(String message, String deviceid) throws PushNotInitializedException;
}
