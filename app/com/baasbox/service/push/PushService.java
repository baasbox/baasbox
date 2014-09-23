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

package com.baasbox.service.push;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import play.Logger;

import com.baasbox.configuration.Push;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.providers.APNServer;
import com.baasbox.service.push.providers.Factory;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.service.push.providers.Factory.VendorOS;
import com.baasbox.service.push.providers.GCMServer;
import com.baasbox.service.push.providers.IPushServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PushService {
	
	private ImmutableMap<ConfigurationKeys, String> getPushParameters(Integer pushProfile){
		ImmutableMap<Factory.ConfigurationKeys,String> response=null;
		if(pushProfile==2){
			if (Push.PROFILE2_PUSH_SANDBOX_ENABLE.getValueAsBoolean()) {
				if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for sandbox environment: 2");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE2_SANDBOX_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE2_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE2_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
				);
			}else{
				if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for production environment: 2");
				response = ImmutableMap.of(
						ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE2_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
						ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE2_PUSH_APPLE_TIMEOUT.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
						ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
						ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
				);		
			}
		}	
		else if(pushProfile==3){
				if (Push.PROFILE3_PUSH_SANDBOX_ENABLE.getValueAsBoolean()) {
					if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for sandbox environment: 3");
					response = ImmutableMap.of(
							ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE3_SANDBOX_ANDROID_API_KEY.getValueAsString(),
							ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE3_PUSH_APPLE_TIMEOUT.getValueAsString(),
							ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE3_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
							ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
							ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
					);
				}else{
					if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for production environment: 3");
					response = ImmutableMap.of(
							ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE3_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
							ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE3_PUSH_APPLE_TIMEOUT.getValueAsString(),
							ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
							ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
							ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
					);		
			}	
			
		}
		else if (Push.PROFILE1_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
			if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for sandbox environment: 1(default)");
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE1_SANDBOX_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE1_PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE, ""+Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
			);
		}else{
			if (Logger.isDebugEnabled()) Logger.debug("Push profile choosen for production environment: 1(default)");
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.PROFILE1_PRODUCTION_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PROFILE1_PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
			);			
		}
		return response;
	}
	
	public boolean[] send(String message, List<String> usernames, List<Integer> pushProfiles, JsonNode bodyJson, boolean[] withError) throws Exception{
		List<String> iosToken = new ArrayList<String>();
		List<String> androidToken = new ArrayList<String>();
		for(String username : usernames) {
			if (Logger.isDebugEnabled()) Logger.debug("Try to send a message (" + message + ") to " + username);
			UserDao udao = UserDao.getInstance();
			ODocument user = udao.getByUserName(username);
			if (user==null) {
				if (Logger.isDebugEnabled()) Logger.debug("User " + username + " does not exist");
				throw new UserNotFoundException("User " + username + " does not exist");
			}
			ODocument userSystemProperties=user.field(UserDao.ATTRIBUTES_SYSTEM);
			if (Logger.isDebugEnabled()) Logger.debug("userSystemProperties: " + userSystemProperties);
			List<ODocument> loginInfos=userSystemProperties.field(UserDao.USER_LOGIN_INFO);
			if (Logger.isDebugEnabled()) Logger.debug("Sending to " + loginInfos.size() + " devices");
			for(ODocument loginInfo : loginInfos){
				String pushToken=loginInfo.field(UserDao.USER_PUSH_TOKEN);
				String vendor=loginInfo.field(UserDao.USER_DEVICE_OS);
				if (Logger.isDebugEnabled()) Logger.debug ("push token: "  + pushToken);
				if (Logger.isDebugEnabled()) Logger.debug ("vendor: "  + vendor);
				if(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(pushToken)){
					VendorOS vos = VendorOS.getVendorOs(vendor);
					if (Logger.isDebugEnabled()) Logger.debug("vos: " + vos);
					if (vos!=null){
						switch(vos) {
							case IOS:
								iosToken.add(pushToken);
								break;
							case ANDROID:
								androidToken.add(pushToken);
								break;
						}
						
						
					} //vos!=null
				}//(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(deviceId)

			}//for (ODocument loginInfo : loginInfos)
		}//for (String username : usernames)
		int i=0;
		for(Integer pushProfile : pushProfiles) {
			APNServer apnServer = new APNServer();
			apnServer.setConfiguration(getPushParameters(pushProfile));
			
			GCMServer gcmServer = new GCMServer();
			gcmServer.setConfiguration(getPushParameters(pushProfile));
			
			if(iosToken.size()>0) withError[i++]=apnServer.send(message, iosToken, bodyJson);
		
			if(androidToken.size()>0) {
				for(List<String> thousandUsers: Lists.partition(androidToken,1000)){ //needed for the GCM sending limit
					withError[i]=gcmServer.send(message, thousandUsers, bodyJson);
				}
				i++;
			}
				
		}
		return withError;
		
	}//send
		

	public boolean validate(List<Integer> pushProfiles) throws IOException, BaasBoxPushException {
		if (pushProfiles.size()>3) throw new PushProfileArrayException("Too many push profiles");
		for(Integer pushProfile : pushProfiles) {
			if((pushProfile!=1) && (pushProfile!=2) && (pushProfile!=3)) throw new PushProfileInvalidException("Error with profiles (accepted values are:1,2 or 3)"); 			
			if((pushProfile==1) && (!Push.PROFILE1_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
			if((pushProfile==2) && (!Push.PROFILE2_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
			if((pushProfile==3) && (!Push.PROFILE3_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
		}
	
		return true;
	}
	
	public boolean validateUser(List<Integer> pushProfiles, VendorOS vendor) throws PushProfileInvalidException, PushProfileDisabledException, PushNotInitializedException {
		for(Integer pushProfile : pushProfiles) {
			if((pushProfile!=1) && (pushProfile!=2) && (pushProfile!=3)) throw new PushProfileInvalidException("Error with profiles (accepted values are:1,2 or 3)"); 			
			switch(vendor) {
				case IOS:
					if(pushProfile==1) {
						if(Push.PROFILE1_PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
							if((Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValue()==null) && (Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.getValue().equals(""))){
								throw new PushNotInitializedException("Configuration not initialized");
							}
						}
						else if((Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValue()==null) && (Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValue().equals(""))){
							throw new PushNotInitializedException("Configuration not initialized");
						}
					}
			}

			if((pushProfile==2) && (!Push.PROFILE2_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
			if((pushProfile==3) && (!Push.PROFILE3_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
		}
		return true;
	}

	
	/*public void sendAll(String message) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException{		 			
	}*/

}
