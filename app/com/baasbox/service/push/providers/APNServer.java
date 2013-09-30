package com.baasbox.service.push.providers;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import play.Logger;
import views.html.admin.main_.content_.assets_.newAsset;

import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.util.ConfigurationFileContainer;
import com.google.common.collect.ImmutableMap;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.apns.EnhancedApnsNotification.*;
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
	public  void send(String message, String deviceid) throws PushNotInitializedException{	
		Logger.debug("APN Push message: "+message+" to the device "+deviceid);
		if (!isInit) throw new PushNotInitializedException("Configuration not initialized");	
		ApnsService service=getService();
		String payload = APNS.newPayload().alertBody(message).build();
		if(timeout<=0){
			try {	
				service.push(deviceid, payload);	
			} catch (NetworkIOException e) {
					Logger.error("Error sending push notification");
					Logger.error(ExceptionUtils.getStackTrace(e));
					//icallbackPush.onError(e.getMessage());
				}
			}
		else {
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
		String name = configuration.get(ConfigurationKeys.IOS_CERTIFICATE);
		File certificate = new IosCertificateHandler().getCertificate(name);
		password=configuration.get(ConfigurationKeys.IOS_CERTIFICATE_PASSWORD);
		sandbox=configuration.get(ConfigurationKeys.IOS_SANDBOX).equalsIgnoreCase("true");
		timeout=Integer.parseInt(configuration.get(ConfigurationKeys.APPLE_TIMEOUT));
		isInit=true;	
		if(certificate.exists() || StringUtils.isNotEmpty(password)) {
			password=configuration.get(ConfigurationKeys.IOS_CERTIFICATE_PASSWORD);
			sandbox=configuration.get(ConfigurationKeys.IOS_SANDBOX).equalsIgnoreCase("true");
			timeout=Integer.parseInt(configuration.get(ConfigurationKeys.APPLE_TIMEOUT));
			isInit=true;	
		}
		else {
			isInit=false;
		}
		
	}


		
}
