package com.baasbox.service.push.providers;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import play.Logger;
import play.mvc.Controller;

import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.ImmutableMap;

public class GCMServer extends Controller implements IPushServer {
	
	private String apikey;
	private boolean isInit=false;


	GCMServer(){
		
	}
	

	
	public void send(String message, String deviceid) throws PushNotInitializedException{
			Logger.debug("GCM Push message: "+message+" to the device "+deviceid);
			if (!isInit) throw new PushNotInitializedException("Configuration not initialized");
			try {

				Sender sender = new Sender(apikey);
				Message msg = new Message.Builder().addData("message", message).build();
            	sender.send(msg,deviceid,1);
			} catch (IOException e) {
				Logger.error("Error sending push notification");
				Logger.error(ExceptionUtils.getStackTrace(e));
				//icallbackPush.onError(e.getMessage());
			}
				//icallbackPush.onSuccess();

	}
	
	
	@Override
	public void setConfiguration(ImmutableMap<Factory.ConfigurationKeys, String> configuration) {
		apikey=configuration.get(ConfigurationKeys.ANDROID_API_KEY);
		isInit=true;
	}



	
	
}

