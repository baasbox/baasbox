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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.Logger;

import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.util.ConfigurationFileContainer;
import com.google.common.collect.ImmutableMap;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.exceptions.NetworkIOException;


public class APNServer  implements IPushServer {
	
	private String certificate;
	private String password;
	private boolean sandbox;
	private int timeout;
	private int identifier;
	private boolean isInit=false;

	APNServer(){
		
	}
	
	  public int INCREMENT_ID() {
	        return ++identifier;
	    }
	
	
	
	@Override
	public  void send(String message, String deviceid, JsonNode bodyJson) throws Exception{	
		if (Logger.isDebugEnabled()) Logger.debug("APN Push message: "+message+" to the device "+deviceid);
		if (!isInit) throw new PushNotInitializedException("Configuration not initialized");	
		
		JsonNode soundNode=bodyJson.findValue("sound");
		String sound =null;
		if (!(soundNode==null)) {
			sound=soundNode.asText();
		}
		
		JsonNode actionLocKeyNode=bodyJson.findValue("actionLocalizedKey"); 
		String actionLocKey=null; 
		
		if (!(actionLocKeyNode==null)) {
			actionLocKey=actionLocKeyNode.asText();
		}
		
		JsonNode locKeyNodes=bodyJson.findValue("localizedKey"); 
		String locKey=null; 
		
		if (!(locKeyNodes==null)) {
			locKey=locKeyNodes.asText();
		}
		
		JsonNode locArgsNode=bodyJson.get("localizedArguments");

		List<String> locArgs = new ArrayList<String>();
		if(!(locArgsNode==null)){
					
			for(JsonNode locArgNode : locArgsNode) {
				locArgs.add(locArgNode.toString());
			}	
		}
						
		JsonNode customDataNodes=bodyJson.get("customData");
		
		Map<String,JsonNode> customData = new HashMap<String,JsonNode>();
				
		if(!(customDataNodes==null)){	
			if(customDataNodes.isArray()) {
				for(JsonNode locArgNode : locArgsNode) {
					locArgs.add(locArgNode.toString());
					customData.put("custom", customDataNodes);
				}	
			}
			else if (customDataNodes.isObject()) {
				JsonNode titleNode=customDataNodes.findValue("title");
				if(titleNode==null) throw new IOException("Error. Key title missing");
				String title=titleNode.asText();
				customData.put(title, customDataNodes);
			}
		}
				
		JsonNode badgeNode=bodyJson.findValue("badge");
		
		int badge=Integer.parseInt(badgeNode.asText());
					
		ApnsService service = null;
		
		try{
			service=getService();
		} catch (com.notnoop.exceptions.InvalidSSLConfig e) {
			Logger.error("Error sending push notification");
			throw new PushNotInitializedException("Error decrypting certificate.Verify your password for given certificate");
			//icallbackPush.onError(e.getMessage());
		}
		
		if (Logger.isDebugEnabled()) Logger.debug("APN Push message: "+message+" to the device "+deviceid +" with sound: " + sound + " with badge: " + badge + " with Action-Localized-Key: " + actionLocKey + " with Localized-Key: "+locKey);
		if (Logger.isDebugEnabled()) Logger.debug("Localized arguments: " + locArgs.toString());
		if (Logger.isDebugEnabled()) Logger.debug("Custom Data: " + customData.toString());


		
		
		String payload = APNS.newPayload()
							.alertBody(message)
							.sound(sound)
							.actionKey(actionLocKey)
							.localizedKey(locKey)
							.localizedArguments(locArgs)
							.badge(badge)
							.customFields(customData)
							.build();
		if(timeout<=0){
			try {	
				service.push(deviceid, payload);	
			} catch (NetworkIOException e) {
					Logger.error("Error sending push notification");
					Logger.error(ExceptionUtils.getStackTrace(e));
					//icallbackPush.onError(e.getMessage());
			}
		} else {
			try {
				EnhancedApnsNotification notification = new EnhancedApnsNotification(INCREMENT_ID(),
				     Integer.MAX_VALUE, deviceid, payload);
				service.push(notification);
			} catch (NetworkIOException e) {
				Logger.error("Error sending enhanced push notification");
				Logger.error(ExceptionUtils.getStackTrace(e));
				//icallbackPush.onError(e.getMessage());
			}
				
		}
		//icallbackPush.onSuccess();
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
