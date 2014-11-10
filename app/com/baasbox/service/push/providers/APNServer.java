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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;
import views.html.defaultpages.badRequest;

import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.service.push.PushNotInitializedException;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.util.ConfigurationFileContainer;
import com.google.common.collect.ImmutableMap;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.exceptions.NetworkIOException;


public class APNServer  extends PushProviderAbstract {

	private String certificate;
	private String password;
	private boolean sandbox;
	private int timeout;
	private boolean isInit=false;

	APNServer(){

	}

	@Override
	public boolean send(String message, List<String> deviceid, JsonNode bodyJson) throws Exception{	
		if (Logger.isDebugEnabled()) Logger.debug("APN Push message: "+message+" to the device "+deviceid);
		if (!isInit) {
			return true;
		}

		ApnsService service = null;
		String payload = null;
		try{
			service=getService();
		} catch (com.notnoop.exceptions.InvalidSSLConfig e) {
			Logger.error("Error sending push notification");
			throw new PushNotInitializedException("Error decrypting certificate.Verify your password for given certificate");
			//icallbackPush.onError(e.getMessage());
		}


		JsonNode contentAvailableNode=bodyJson.findValue("content-available");
		Integer contentAvailable = null;
		if(!(contentAvailableNode == null)) {
			if(!(contentAvailableNode.isInt())) throw new PushContentAvailableFormatException("Content-available MUST be an Integer (1 for silent notification)");
			contentAvailable=contentAvailableNode.asInt();
		}
		if(contentAvailable!=1) {

			JsonNode categoryNode=bodyJson.findValue("category");
			String category = null;
			if(!(categoryNode == null)) {
				if(!(categoryNode.isTextual())) throw new PushCategoryFormatException("Category MUST be a String");
				category=categoryNode.asText();
			}

			JsonNode soundNode=bodyJson.findValue("sound");
			String sound =null;
			if (!(soundNode==null)) {
				if(!(soundNode.isTextual())) throw new PushSoundKeyFormatException("Sound value MUST be a String");
				sound=soundNode.asText();
			}

			JsonNode actionLocKeyNode=bodyJson.findValue("actionLocalizedKey"); 
			String actionLocKey=null; 

			if (!(actionLocKeyNode==null)) {
				if(!(actionLocKeyNode.isTextual())) throw new PushActionLocalizedKeyFormatException("ActionLocalizedKey MUST be a String");
				actionLocKey=actionLocKeyNode.asText();
			}

			JsonNode locKeyNode=bodyJson.findValue("localizedKey"); 
			String locKey=null; 

			if (!(locKeyNode==null)) {
				if(!(locKeyNode.isTextual())) throw new PushLocalizedKeyFormatException("LocalizedKey MUST be a String");
				locKey=locKeyNode.asText();
			}

			JsonNode locArgsNode=bodyJson.get("localizedArguments");

			List<String> locArgs = new ArrayList<String>();
			if(!(locArgsNode==null)){
				if(!(locArgsNode.isArray())) throw new PushLocalizedArgumentsFormatException("LocalizedArguments MUST be an Array of String");		
				for(JsonNode locArgNode : locArgsNode) {
					if(locArgNode.isNumber()) throw new PushLocalizedArgumentsFormatException("LocalizedArguments MUST be an Array of String");
					locArgs.add(locArgNode.toString());
				}	
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

			JsonNode badgeNode=bodyJson.findValue("badge");
			int badge=0;
			if(!(badgeNode==null)) {
				if(!(badgeNode.isNumber())) throw new PushBadgeFormatException("Badge value MUST be a number");
				else badge=badgeNode.asInt();
			}

			if (Logger.isDebugEnabled()) Logger.debug("APN Push message: "+message+" to the device "+deviceid +" with sound: " + sound + " with badge: " + badge + " with Action-Localized-Key: " + actionLocKey + " with Localized-Key: "+locKey);
			if (Logger.isDebugEnabled()) Logger.debug("Localized arguments: " + locArgs.toString());
			if (Logger.isDebugEnabled()) Logger.debug("Custom Data: " + customData.toString());


			payload = APNS.newPayload()
					.alertBody(message)
					.sound(sound)
					.actionKey(actionLocKey)
					.localizedKey(locKey)
					.localizedArguments(locArgs)
					.badge(badge)
					.customFields(customData)
					.category(category)
					.build();
		}

		else {
			payload=APNS.newPayload()
					.instantDeliveryOrSilentNotification()
					.build();

		}

		if(timeout<=0){
			try {	
				service.push(deviceid, payload);	
			} catch (NetworkIOException e) {
				Logger.error("Error sending push notification");
				Logger.error(ExceptionUtils.getStackTrace(e));
				throw new PushNotInitializedException("Error processing certificate, maybe it's revoked");
				//icallbackPush.onError(e.getMessage());
			}
		} else {
			try {
				Date expiry = new Date(Long.MAX_VALUE);
				service.push(deviceid,payload,expiry);
			} catch (NetworkIOException e) {
				Logger.error("Error sending enhanced push notification");
				Logger.error(ExceptionUtils.getStackTrace(e));
				throw new PushNotInitializedException("Error processing certificate, maybe it's revoked");
				//icallbackPush.onError(e.getMessage());
			}

		}
		//icallbackPush.onSuccess();
		return false;
	}


	public static boolean validatePushPayload(JsonNode bodyJson) throws BaasBoxPushException {
		JsonNode soundNode=bodyJson.findValue("sound");


		JsonNode contentAvailableNode=bodyJson.findValue("content-available");
		Integer contentAvailable = null;
		if(!(contentAvailableNode == null)) {
			if(!(contentAvailableNode.isInt())) throw new PushContentAvailableFormatException("Content-available MUST be an Integer (1 for silent notification)");
			contentAvailable=contentAvailableNode.asInt();
		}

		JsonNode categoryNode=bodyJson.findValue("category");
		String category = null;
		if(!(categoryNode == null)) {
			if(!(categoryNode.isTextual())) throw new PushCategoryFormatException("Category MUST be a String");
			category=categoryNode.asText();
		}


		String sound =null;
		if (!(soundNode==null)) {
			if(!(soundNode.isTextual())) throw new PushSoundKeyFormatException("Sound value MUST be a String");
			sound=soundNode.asText();
		}

		JsonNode actionLocKeyNode=bodyJson.findValue("actionLocalizedKey"); 
		String actionLocKey=null; 

		if (!(actionLocKeyNode==null)) {
			if(!(actionLocKeyNode.isTextual())) throw new PushActionLocalizedKeyFormatException("ActionLocalizedKey MUST be a String");
			actionLocKey=actionLocKeyNode.asText();
		}

		JsonNode locKeyNode=bodyJson.findValue("localizedKey"); 
		String locKey=null; 

		if (!(locKeyNode==null)) {
			if(!(locKeyNode.isTextual())) throw new PushLocalizedKeyFormatException("LocalizedKey MUST be a String");
			locKey=locKeyNode.asText();
		}

		JsonNode locArgsNode=bodyJson.get("localizedArguments");

		List<String> locArgs = new ArrayList<String>();
		if(!(locArgsNode==null)){
			if(!(locArgsNode.isArray())) throw new PushLocalizedArgumentsFormatException("LocalizedArguments MUST be an Array of String");		
			for(JsonNode locArgNode : locArgsNode) {
				if(!locArgNode.isTextual()) throw new PushLocalizedArgumentsFormatException("LocalizedArguments MUST be an Array of String");
				locArgs.add(locArgNode.toString());
			}	
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

		JsonNode badgeNode=bodyJson.findValue("badge");
		int badge=0;
		if(!(badgeNode==null)) {
			if(!(badgeNode.isNumber())) throw new PushBadgeFormatException("Badge value MUST be a number");
			else badge=badgeNode.asInt();
		}

		return true;
	}




	private  ApnsService getService() {
		ApnsService service;
		if (!sandbox) service=APNS.newService()
				.withCert(certificate, password).withProductionDestination().build();
		else  service=APNS.newService()
				.withCert(certificate, password)
				.withSandboxDestination()
				.build();
		return service;
	}

	@Override
	public void setConfiguration(ImmutableMap<ConfigurationKeys, String> configuration) {
		String json = configuration.get(ConfigurationKeys.IOS_CERTIFICATE);
		String name = null;
		ObjectMapper mp = new ObjectMapper();
		try{
			ConfigurationFileContainer cfc = mp.readValue(json, ConfigurationFileContainer.class);
			name = cfc.getName();
		}catch(Exception e){
			Logger.error(e.getMessage());
		}
		if(name!=null && !name.equals("null")){
			File f = IosCertificateHandler.getCertificate(name);
			this.certificate=f.getAbsolutePath();
		}
		password=configuration.get(ConfigurationKeys.IOS_CERTIFICATE_PASSWORD);
		sandbox=configuration.get(ConfigurationKeys.IOS_SANDBOX).equalsIgnoreCase("true");
		timeout=Integer.parseInt(configuration.get(ConfigurationKeys.APPLE_TIMEOUT));
		isInit=StringUtils.isNotEmpty(this.certificate) && StringUtils.isNotEmpty(password);	
	}



}
