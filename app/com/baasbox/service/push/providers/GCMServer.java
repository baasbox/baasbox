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

import static com.google.android.gcm.server.Constants.JSON_PAYLOAD;
import static com.google.android.gcm.server.Constants.JSON_REGISTRATION_IDS;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONValue;

import play.Logger;

import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Sender;
import com.google.common.collect.ImmutableMap;


public class GCMServer extends PushProviderAbstract {

	private String apikey;
	private boolean isInit = false;
	private final static int MAX_TIME_TO_LIVE=2419200;  //4 WEEKS

	GCMServer() {

	}

	public boolean send(String message, List<String> deviceid, JsonNode bodyJson)
			throws PushNotInitializedException, InvalidRequestException, UnknownHostException,IOException, PushTimeToLiveFormatException, PushCollapseKeyFormatException {
		if (Logger.isDebugEnabled()) Logger.debug("GCM Push message: "+message+" to the device "+deviceid);
		if (!isInit) {
			return true;
		}
		JsonNode customDataNodes=bodyJson.get("custom");

		Map<String,JsonNode> customData = new HashMap<String,JsonNode>();

		if(!(customDataNodes==null)){
			if(customDataNodes.isTextual()) {
				customData.put("custom",customDataNodes);
			}
			else {
				for(JsonNode customDataNode : customDataNodes) {
					customData.put("custom", customDataNodes);
				}
			}
		}

		JsonNode collapse_KeyNode=bodyJson.findValue("collapse_key"); 
		String collapse_key=null; 

		if(!(collapse_KeyNode==null)) {
			if(!(collapse_KeyNode.isTextual())) throw new PushCollapseKeyFormatException("Collapse_key MUST be a String");
			collapse_key=collapse_KeyNode.asText();
		}
		else collapse_key="";

		JsonNode timeToLiveNode=bodyJson.findValue("time_to_live");
		int time_to_live = 0;

		if(!(timeToLiveNode==null)) {
			if(!(timeToLiveNode.isNumber())) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
			else if(timeToLiveNode.asInt() < 0) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
			else if(timeToLiveNode.asInt()>MAX_TIME_TO_LIVE){
				time_to_live=MAX_TIME_TO_LIVE;
			}
			else time_to_live=timeToLiveNode.asInt();

		}
		else time_to_live=MAX_TIME_TO_LIVE; //IF NULL WE SET DEFAULT VALUE (4 WEEKS)

		if (Logger.isDebugEnabled()) Logger.debug("collapse_key: " + collapse_key.toString());

		if (Logger.isDebugEnabled()) Logger.debug("time_to_live: " + time_to_live);

		if (Logger.isDebugEnabled()) Logger.debug("Custom Data: " + customData.toString());

		Sender sender = new Sender(apikey);
		Message msg = new Message.Builder().addData("message", message)
				.addData("custom", customData.toString())
				.collapseKey(collapse_key.toString())
				.timeToLive(time_to_live)
				.build();

		sender.send(msg, deviceid , 1);


		// icallbackPush.onError(e.getMessage());

		// icallbackPush.onSuccess();
		return false;

	}

	public static boolean validatePushPayload(JsonNode bodyJson) throws BaasBoxPushException {
		JsonNode collapse_KeyNode=bodyJson.findValue("collapse_key"); 

		if(!(collapse_KeyNode==null)) {
			if(!(collapse_KeyNode.isTextual())) throw new PushCollapseKeyFormatException("Collapse_key MUST be a String");
		}

		JsonNode timeToLiveNode=bodyJson.findValue("time_to_live");

		if(!(timeToLiveNode==null)) {
			if(!(timeToLiveNode.isNumber())) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
			else if(timeToLiveNode.asInt() < 0) throw new PushTimeToLiveFormatException("Time_to_live MUST be a positive number or equal zero");
		}
		return true;

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
