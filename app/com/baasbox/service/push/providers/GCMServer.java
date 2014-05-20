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

package com.baasbox.service.push.providers;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import play.Logger;
import play.mvc.Controller;

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
			throws PushNotInitializedException, InvalidRequestException, UnknownHostException,IOException {
		if (Logger.isDebugEnabled()) Logger.debug("GCM Push message: " + message + " to the device "
				+ deviceid);
		if (!isInit)
			throw new PushNotInitializedException(
					"Configuration not initialized");
			Sender sender = new Sender(apikey);
			Message msg = new Message.Builder().addData("message", message)
					.build();

			sender.send(msg, deviceid, 1);
		

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
