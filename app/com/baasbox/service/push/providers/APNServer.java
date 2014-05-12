package com.baasbox.service.push.providers;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
	public  void send(String message, String deviceid) throws PushNotInitializedException{	
		if (Logger.isDebugEnabled()) Logger.debug("APN Push message: "+message+" to the device "+deviceid);
		if (!isInit) throw new PushNotInitializedException("Configuration not initialized");	
		ApnsService service = null;
		try{
			service=getService();
		} catch (com.notnoop.exceptions.InvalidSSLConfig e) {
			Logger.error("Error sending push notification");
			throw new PushNotInitializedException("Error decrypting certificate.Verify your password for given certificate");
			//icallbackPush.onError(e.getMessage());
		}
		String payload = APNS.newPayload().alertBody(message).build();
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
