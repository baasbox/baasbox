package com.baasbox.service.push.providers;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import play.Logger;
import play.mvc.Controller;

import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.ImmutableMap;

public class GCMServer extends Controller implements IPushServer {

	private String apikey;
	private boolean isInit = false;

	GCMServer() {

	}

	public void send(String message, String deviceid)
			throws PushNotInitializedException, InvalidRequestException, UnknownHostException {
		Logger.debug("GCM Push message: " + message + " to the device "
				+ deviceid);
		if (!isInit)
			throw new PushNotInitializedException(
					"Configuration not initialized");
		try {
			Sender sender = new Sender(apikey);
			Message msg = new Message.Builder().addData("message", message)
					.build();

			sender.send(msg, deviceid, 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		// icallbackPush.onError(e.getMessage());

		// icallbackPush.onSuccess();

	}

	@Override
	public void setConfiguration(
			ImmutableMap<Factory.ConfigurationKeys, String> configuration) {
		apikey = configuration.get(ConfigurationKeys.ANDROID_API_KEY);
		if (StringUtils.isNotEmpty(apikey)) {
			isInit = true;
		}
	}

}
