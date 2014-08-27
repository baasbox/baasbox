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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONValue;

import play.Logger;
import play.mvc.Controller;

import com.baasbox.service.push.PushInvalidApiKeyException;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.ImmutableMap;

import static com.google.android.gcm.server.Constants.GCM_SEND_ENDPOINT;
import static com.google.android.gcm.server.Constants.JSON_PAYLOAD;
import static com.google.android.gcm.server.Constants.JSON_REGISTRATION_IDS;
import static com.google.android.gcm.server.Constants.PARAM_COLLAPSE_KEY;
import static com.google.android.gcm.server.Constants.PARAM_DELAY_WHILE_IDLE;
import static com.google.android.gcm.server.Constants.PARAM_TIME_TO_LIVE;


public class GCMServer extends Controller implements IPushServer {

	private String apikey;
	private boolean isInit = false;

	public GCMServer() {

	}

	public void send(String message, List<String> deviceid, JsonNode bodyJson)
			throws PushNotInitializedException, InvalidRequestException, UnknownHostException,IOException {
		if (Logger.isDebugEnabled()) Logger.debug("GCM Push message: "+message+" to the device "+deviceid);
		if (!isInit)
			throw new PushNotInitializedException(
					"Configuration not initialized");
		JsonNode customDataNodes=bodyJson.get("customData");
		
		Map<String,JsonNode> customData = new HashMap<String,JsonNode>();
				
		if(!(customDataNodes==null)){	
				
			for(JsonNode customDataNode : customDataNodes) {
				customData.put("custom", customDataNodes);
			}	
		}
		if (Logger.isDebugEnabled()) Logger.debug("Custom Data: " + customData.toString());

			Sender sender = new Sender(apikey);
			Message msg = new Message.Builder().addData("message", message).addData("custom", customData.toString())
					.build();

			sender.send(msg, deviceid , 1);
		

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
	
	public static void validateApiKey(String apikey) throws MalformedURLException, IOException, PushInvalidApiKeyException{
	    Message message = new Message.Builder().addData("message", "validateAPIKEY")
	    		.build();
	    Sender sender = new Sender(apikey);
	    
	    List<String> deviceid = new ArrayList<String>();
	    deviceid.add("ABC");
	    
	    Map<Object, Object> jsonRequest = new HashMap<Object, Object>();
	    jsonRequest.put(JSON_REGISTRATION_IDS, deviceid);
	    Map<String, String> payload = message.getData();
	    if (!payload.isEmpty()) {
	      jsonRequest.put(JSON_PAYLOAD, payload);
	    }
	    String requestBody = JSONValue.toJSONString(jsonRequest);
	    
	    String url=com.google.android.gcm.server.Constants.GCM_SEND_ENDPOINT;
	    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		
	    byte[] bytes = requestBody.getBytes();
	    
	    conn.setDoOutput(true);
		conn.setUseCaches(false);
		conn.setFixedLengthStreamingMode(bytes.length);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "key=" + apikey);
		OutputStream out = conn.getOutputStream();
		out.write(bytes);
		out.close();
	
	    int status = conn.getResponseCode();
	    if (status != 200) {
	    	if (status == 401) {
	        	throw new PushInvalidApiKeyException("Wrong api key");
	        }
	        if (status == 503) {
	        	throw new UnknownHostException();
	        }
	    }
		
		
	}
	
	
}
