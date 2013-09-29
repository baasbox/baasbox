package com.baasbox.service.push;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import play.Logger;
import play.mvc.Http;
import play.mvc.Http.Context;

import com.baasbox.configuration.Push;
import com.baasbox.dao.UserDao;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.push.providers.Factory;
import com.baasbox.service.push.providers.Factory.ConfigurationKeys;
import com.baasbox.service.push.providers.Factory.VendorOS;
import com.baasbox.service.push.providers.IPushServer;
import com.baasbox.service.push.providers.PushNotInitializedException;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;
import com.google.android.gcm.server.InvalidRequestException;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PushService {
	
	private ImmutableMap<ConfigurationKeys, String> getPushParameters(){
		ImmutableMap<Factory.ConfigurationKeys,String> response=null;
		if (Push.PUSH_SANDBOX_ENABLE.getValueAsBoolean()){
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.SANDBOX_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE, ""+Push.SANDBOX_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.SANDBOX_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.TRUE.toString()
			);
		}else{
			response = ImmutableMap.of(
					ConfigurationKeys.ANDROID_API_KEY, ""+Push.PRODUCTION_ANDROID_API_KEY.getValueAsString(),
					ConfigurationKeys.APPLE_TIMEOUT, ""+Push.PUSH_APPLE_TIMEOUT.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE,""+ Push.PRODUCTION_IOS_CERTIFICATE.getValueAsString(),
					ConfigurationKeys.IOS_CERTIFICATE_PASSWORD, ""+Push.PRODUCTION_IOS_CERTIFICATE_PASSWORD.getValueAsString(),
					ConfigurationKeys.IOS_SANDBOX,""+Boolean.FALSE.toString()
			);			
		}
		return response;
	}
	
	public void send(String message, String username) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException, InvalidRequestException, InvalidRequestException, UnknownHostException{
		Logger.debug("Try to send a message (" + message + ") to " + username);
		UserDao udao = UserDao.getInstance();
		ODocument user = udao.getByUserName(username);
		if (user==null) {
			Logger.debug("User " + username + " does not exist");
			throw new UserNotFoundException("User " + username + " does not exist");
		}
		ODocument userSystemProperties = user.field(UserDao.ATTRIBUTES_SYSTEM);
		Logger.debug("userSystemProperties: " + userSystemProperties);
		List<ODocument> loginInfos=userSystemProperties.field(UserDao.USER_LOGIN_INFO);
		Logger.debug("Sending to " + loginInfos.size() + " devices");
		for(ODocument loginInfo : loginInfos){
			String deviceId=loginInfo.field(UserDao.USER_DEVICE_ID);
			String vendor=loginInfo.field(UserDao.USER_DEVICE_OS);
			Logger.debug ("deviceId: "  + deviceId);
			Logger.debug ("vendor: "  + vendor);			
			if(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(deviceId)){
				VendorOS vos = VendorOS.getVendorOs(vendor);
				Logger.debug("vos: " + vos);
				if (vos!=null){
					IPushServer pushServer = Factory.getIstance(vos);
					pushServer.setConfiguration(getPushParameters());
					pushServer.send(message, deviceId);
				} //vos!=null
			}//(!StringUtils.isEmpty(vendor) && !StringUtils.isEmpty(deviceId)

		}//for (ODocument loginInfo : loginInfos)
	}//send
		
	
	/*public void sendAll(String message) throws PushNotInitializedException, UserNotFoundException, SqlInjectionException{		 			
	}*/

}
