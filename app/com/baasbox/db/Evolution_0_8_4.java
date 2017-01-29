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

package com.baasbox.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.baasbox.configuration.Push;
import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.IndexNotFoundException;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.util.ConfigurationFileContainer;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.mvrbtree.OMVRBTree.EntrySet;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Evolution_0_8_4 implements IEvolution {
	private String version="0.8.4";
	
	public Evolution_0_8_4() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		BaasBoxLogger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			registeredRoleInheritsFromAnonymousRole(db);
			updateDefaultTimeFormat(db);
//            addScriptsClass(db);
//            addScriptsPermission();
			multiPushProfileSettings(db);
			exposeSocialId(db);
		}catch (Throwable e){
			BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		BaasBoxLogger.info ("DB now is on " + version + " level");
	}



	
	//issue #195 Registered users should have access to anonymous resources
		private void registeredRoleInheritsFromAnonymousRole(ODatabaseRecordTx db) {
			BaasBoxLogger.info("...updating registered role");
			
			RoleDao.getRole(DefaultRoles.ADMIN.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("admin").getDocument().getRecord() ).save();
			RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("writer").getDocument().getRecord() ).save();
			RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("anonymous").getDocument().getRecord() ).save();
			RoleDao.getRole(DefaultRoles.BACKOFFICE_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("writer").getDocument().getRecord() ).save();
			
			
			RoleDao.getRole(DefaultRoles.BASE_READER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
			RoleDao.getRole(DefaultRoles.BASE_WRITER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
			RoleDao.getRole(DefaultRoles.BASE_ADMIN.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
			
			db.getMetadata().reload();
			BaasBoxLogger.info("...done");
		}
	
	private void updateDefaultTimeFormat(ODatabaseRecordTx db) {
			DbHelper.execMultiLineCommands(db,true,"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	}
	
	/***
	 * creates new records for new push settings and migrates the old ones into the profile n.1
	 * @param db
	 */
	private void multiPushProfileSettings(ODatabaseRecordTx db) {
		IndexPushConfiguration idx;
		try {
			idx = new IndexPushConfiguration();
		} catch (IndexNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		//load the old settings
		String sandbox=null;
		if(idx.get("push.sandbox.enable")==null) sandbox="true";
		else sandbox=idx.get("push.sandbox.enable").toString();
		//String sandbox = StringUtils.defaultString(()idx.get("push.sandbox.enable"),"true");
		String appleTimeout=null;
		if(idx.get("push.apple.timeout")==null) appleTimeout="0";
		else appleTimeout=idx.get("push.apple.timeout").toString();
		
		//StringUtils.defaultString((String)idx.get("push.apple.timeout"),null);
		
		String sandboxAndroidApiKey= StringUtils.defaultString((String)idx.get("sandbox.android.api.key"),null);
		String sandBoxIosCertificatePassword = StringUtils.defaultString((String)idx.get("sandbox.ios.certificate.password"),null);
		
		String prodAndroidApiKey= StringUtils.defaultString((String)idx.get("production.android.api.key"),null);
		String prodBoxIosCertificatePassword = StringUtils.defaultString((String)idx.get("production.ios.certificate.password"),null);
		
		//Houston we have a problem. Here we have to handle the iOS certificates that are files!
		Object sandBoxIosCertificate = getValueAsFileContainer(idx.get("sandbox.ios.certificate"));
		Object prodBoxIosCertificate = getValueAsFileContainer(idx.get("production.ios.certificate"));
		
		try{
			//set the new profile1 settings
			Push.PROFILE1_PRODUCTION_ANDROID_API_KEY._setValue(prodAndroidApiKey);
			if(prodBoxIosCertificate != null) Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE._setValue(prodBoxIosCertificate);
			Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD._setValue(prodBoxIosCertificatePassword);
			Push.PROFILE1_PUSH_APPLE_TIMEOUT._setValue(appleTimeout);
			Push.PROFILE1_SANDBOX_ANDROID_API_KEY._setValue(sandboxAndroidApiKey);
			if (sandBoxIosCertificate != null) Push.PROFILE1_SANDBOX_IOS_CERTIFICATE._setValue(sandBoxIosCertificate);
			Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD._setValue(sandBoxIosCertificatePassword);
			Push.PROFILE1_PUSH_SANDBOX_ENABLE._setValue(sandbox);
			
			Push.PROFILE1_PUSH_PROFILE_ENABLE.setValue(false);
			try{
				Push.PROFILE1_PUSH_PROFILE_ENABLE.setValue(true);
			}catch (Exception e){
				Push.PROFILE1_PUSH_PROFILE_ENABLE.setValue(false);
			}
			
			//disable other profiles
			Push.PROFILE2_PUSH_PROFILE_ENABLE._setValue(false);
			Push.PROFILE3_PUSH_PROFILE_ENABLE._setValue(false);
			
			//default value other profiles
			Push.PROFILE2_PUSH_SANDBOX_ENABLE._setValue(true);
			Push.PROFILE2_PRODUCTION_ANDROID_API_KEY._setValue("");
			//Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE
			Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE_PASSWORD._setValue("");
			Push.PROFILE2_PUSH_APPLE_TIMEOUT._setValue(0);
			Push.PROFILE2_SANDBOX_ANDROID_API_KEY._setValue("");
			//Push.PROFILE2_SANDBOX_IOS_CERTIFICATE
			Push.PROFILE2_SANDBOX_IOS_CERTIFICATE_PASSWORD._setValue("");
			
			Push.PROFILE3_PUSH_SANDBOX_ENABLE._setValue(true);
			Push.PROFILE3_PRODUCTION_ANDROID_API_KEY._setValue("");
			//Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE
			Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE_PASSWORD._setValue("");
			Push.PROFILE3_PUSH_APPLE_TIMEOUT._setValue(0);
			Push.PROFILE3_SANDBOX_ANDROID_API_KEY._setValue("");
			//Push.PROFILE3_SANDBOX_IOS_CERTIFICATE
			Push.PROFILE3_SANDBOX_IOS_CERTIFICATE_PASSWORD._setValue("");
		}catch (Exception e){
			throw new RuntimeException(e);
		}	
	}
	
	//this is needed due iOS certificates migrations
	private ConfigurationFileContainer getValueAsFileContainer(Object v) {
		ConfigurationFileContainer result = null;
		if(v!=null){
			ObjectMapper om = BBJson.mapper();
			try {
				result = om.readValue(v.toString(), ConfigurationFileContainer.class);
			} catch (Exception e) {
				e.printStackTrace();
				return result;
			}
		}
		return result;
	}




	
	
	/***
	 * expose some attributes when users logged in via social network
	 * @param db
	 * @throws DocumentNotFoundException 
	 * @throws InvalidModelException 
	 */
	private void exposeSocialId(ODatabaseRecordTx db) throws InvalidModelException, DocumentNotFoundException {
		//take all users and expose their generated flag
		//remove permission for registered users to system attributes
		//put the signupdate in user system info
		//put the _social data structure into the visiblyByRegisteredUsers attribute
		DbHelper.execMultiLineCommands(db, true, new String [] 
				{
					"update _bb_user set generated = system.generated_username;",
					"update _bb_user set system._allowRead = [];",
					"update _bb_user set system.signUpDate=signUpDate;"
				} 
		);
		BaasBoxLogger.info("...moving social ids. This can take several minutes....");
		Object listOfUser=DbHelper.genericSQLStatementExecute("select @rid,system.sso_tokens as social_network from _bb_user where system.sso_tokens is not null", new String[]{""});
		for (Object doc: (List)listOfUser){
			UserDao udao= UserDao.getInstance();

			ODocument odoc = (ODocument)doc;
			ODocument profile;
			if (odoc.field("rid") instanceof ODocument){
				profile = odoc.field("rid");
			} else {
				ORID rid=odoc.field("rid");
				profile = udao.get(rid);
			}
			OTrackedMap sn=odoc.field("social_network");
			
			ODocument registeredUserProp = profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
			if (registeredUserProp==null) registeredUserProp=new ODocument(UserDao.USER_ATTRIBUTES_CLASS);
			
			Map socialdata=registeredUserProp.field("_social");
			if(socialdata == null){
				socialdata = new HashMap<String,Map>();
			}
			Set socialNetworks = sn.keySet();
			for (Object socialNetwork : socialNetworks) {
				ODocument socialInfo = (ODocument)sn.get(socialNetwork);
				Map dataMap = new HashMap<String,String>();
				dataMap.put("id", socialInfo.field("id"));
				socialdata.put(socialNetwork,dataMap);
			}
			
			/*
			for (int i=0;i<sn.fields();i++){
				String socialNetwork = sn.fieldNames()[i];
				ODocument socialInfo = (ODocument)sn.fieldValues()[i];
				socialdata.put(socialNetwork, (ODocument)new ODocument().fromJSON("{\"id\":\""+ socialInfo.field("id") +"\"}"));
			}
			*/

			registeredUserProp.field("_social",socialdata);
			registeredUserProp.save();
			profile.save();
		}
	}

}