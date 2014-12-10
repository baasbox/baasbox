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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import play.Logger;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Application;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.configuration.Push;
import com.baasbox.dao.UserDao;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.providers.APNServer;
import com.baasbox.service.push.providers.Factory;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.service.push.providers.Factory.VendorOS;
import com.baasbox.service.push.providers.GCMServer;
import com.baasbox.service.push.providers.IPushServer;
import com.baasbox.service.push.providers.PushProviderAbstract;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.baasbox.service.push.PushMailException;
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
		com.baasbox.db.DbHelper.reconnectAsAdmin();
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
			
			if(loginInfos.isEmpty()) {
				// send email to the Guy
				ODocument attrObj =user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
				if (attrObj == null || attrObj.field("email") == null) break;
				else {
					sendPushMail(user);		
					break;
				}
			}
			
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
			HashMap<Factory.VendorOS,IPushServer> allVendors= Factory.getAllIstances();

			IPushServer apnServer =  allVendors.get(VendorOS.IOS);
			apnServer.setConfiguration(getPushParameters(pushProfile));

			IPushServer gcmServer =  allVendors.get(VendorOS.ANDROID);
			gcmServer.setConfiguration(getPushParameters(pushProfile));

			if(iosToken.size()>0) {
				for(List<String> thousandUsersApple : Lists.partition(iosToken, 1000)){
					withError[i]=apnServer.send(message, thousandUsersApple, bodyJson);
				}
				i++;
			}

			if(androidToken.size()>0) {
				for(List<String> thousandUsersAndroid: Lists.partition(androidToken,1000)){ //needed for the GCM sending limit
					withError[i]=gcmServer.send(message, thousandUsersAndroid, bodyJson);
				}
				i++;
			}

		}
		com.baasbox.db.DbHelper.reconnectAsAuthenticatedUser();
		return withError;

	}//send


	private void sendPushMail(ODocument user) throws Exception {
		//initialization
		String siteUrl = Application.NETWORK_HTTP_URL.getValueAsString();
		int sitePort = Application.NETWORK_HTTP_PORT.getValueAsInteger();
		if (StringUtils.isEmpty(siteUrl)) throw  new PushMailException( "invalid site url (is empty)");

		String textEmail = PasswordRecovery.EMAIL_TEMPLATE_TEXT.getValueAsString();
		String htmlEmail = PasswordRecovery.EMAIL_TEMPLATE_HTML.getValueAsString();
		if (StringUtils.isEmpty(htmlEmail)) htmlEmail=textEmail;
		if (StringUtils.isEmpty(htmlEmail)) throw  new PushMailException ("text to send is not configured");

		boolean useSSL = PasswordRecovery.NETWORK_SMTP_SSL.getValueAsBoolean();
		boolean useTLS = PasswordRecovery.NETWORK_SMTP_TLS.getValueAsBoolean();
		String smtpHost = PasswordRecovery.NETWORK_SMTP_HOST.getValueAsString();
		int smtpPort = PasswordRecovery.NETWORK_SMTP_PORT.getValueAsInteger();
		if (StringUtils.isEmpty(smtpHost) || (smtpHost.equals("mail.example.com"))) throw new PushMailException ("SMTP host is not configured");

		String username_smtp = null;
		String password_smtp = null;
		if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
			username_smtp = PasswordRecovery.NETWORK_SMTP_USER.getValueAsString();
			password_smtp = PasswordRecovery.NETWORK_SMTP_PASSWORD.getValueAsString();
			if (StringUtils.isEmpty(username_smtp)) throw  new PushMailException ("SMTP username is not configured");
		}
		
		String emailFrom = PasswordRecovery.EMAIL_FROM.getValueAsString();
		String emailSubject = "Push notification received";
		if (StringUtils.isEmpty(emailFrom)) throw  new PushMailException ("Sender email is not configured");

		
		try {
			String userEmail=((ODocument) user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER)).field("email").toString();

			HtmlEmail email = null;
			
			email = new HtmlEmail();
			
			//Email Configuration
			email.setSSL(useSSL);
			email.setSSLOnConnect(useSSL);
			email.setTLS(useTLS);
			email.setStartTLSEnabled(useTLS);
			email.setStartTLSRequired(useTLS);
			email.setSSLCheckServerIdentity(false);
			email.setSslSmtpPort(String.valueOf(smtpPort));   
			email.setHostName(smtpHost);
			email.setSmtpPort(smtpPort);
			email.setCharset("utf-8");
			
			if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
				email.setAuthenticator(new  DefaultAuthenticator(username_smtp, password_smtp));
			}
			email.setFrom(emailFrom);			
			email.addTo(userEmail);

			email.setSubject(emailSubject);
			if (Logger.isDebugEnabled()) {
				StringBuilder logEmail = new StringBuilder()
						.append("HostName: ").append(email.getHostName()).append("\n")
						.append("SmtpPort: ").append(email.getSmtpPort()).append("\n")
						.append("SslSmtpPort: ").append(email.getSslSmtpPort()).append("\n")
						
						.append("SSL: ").append(email.isSSL()).append("\n")
						.append("TLS: ").append(email.isTLS()).append("\n")						
						.append("SSLCheckServerIdentity: ").append(email.isSSLCheckServerIdentity()).append("\n")
						.append("SSLOnConnect: ").append(email.isSSLOnConnect()).append("\n")
						.append("StartTLSEnabled: ").append(email.isStartTLSEnabled()).append("\n")
						.append("StartTLSRequired: ").append(email.isStartTLSRequired()).append("\n")
						
						.append("SubType: ").append(email.getSubType()).append("\n")
						.append("SocketConnectionTimeout: ").append(email.getSocketConnectionTimeout()).append("\n")
						.append("SocketTimeout: ").append(email.getSocketTimeout()).append("\n")
						
						.append("FromAddress: ").append(email.getFromAddress()).append("\n")
						.append("ReplyTo: ").append(email.getReplyToAddresses()).append("\n")
						.append("BCC: ").append(email.getBccAddresses()).append("\n")
						.append("CC: ").append(email.getCcAddresses()).append("\n")
						
						.append("Subject: ").append(email.getSubject()).append("\n")

						//the following line throws a NPE in debug mode
						//.append("Message: ").append(email.getMimeMessage().getContent()).append("\n")

						
						.append("SentDate: ").append(email.getSentDate()).append("\n");
				Logger.debug("PushMail is ready to send: \n" + logEmail.toString());
			}
			email.send();

		}  catch (EmailException authEx){
			Logger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(authEx));
			throw new PushMailException ("Could not reach the mail server. Please contact the server administrator");
		}  catch (Exception e) {
			Logger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(e));
			throw new Exception ("Error on sending pushMail");
		}
	
	}

	public boolean validate(List<Integer> pushProfiles) throws IOException, BaasBoxPushException {
		for(Integer pushProfile : pushProfiles) {
			if((pushProfile!=1) && (pushProfile!=2) && (pushProfile!=3)) throw new PushProfileInvalidException("Error with profiles (accepted values are:1,2 or 3)"); 			
			if (!isMocked()){
				if((pushProfile==1) && (!Push.PROFILE1_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
				if((pushProfile==2) && (!Push.PROFILE2_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
				if((pushProfile==3) && (!Push.PROFILE3_PUSH_PROFILE_ENABLE.getValueAsBoolean())) throw new PushProfileDisabledException("Profile not enabled"); 
			}//isMocked
		}
		return true; 
	}

	public boolean isMocked(){
		if (BBConfiguration.getPushMock()) Logger.warn("PushService is Mocked!!");
		return BBConfiguration.getPushMock();
	}
	

	/*public void sendAll(String message) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException{		 			
	}*/

}
