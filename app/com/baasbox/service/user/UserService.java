/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.service.user;


import java.net.URL;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.codehaus.jackson.JsonNode;
import org.stringtemplate.v4.ST;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Application;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.ResetPwdDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.ResetPasswordException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.service.push.PushService;
import com.baasbox.service.role.RoleService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;


public class UserService {

	public static void createDefaultUsers(){
		try{
			//the baasbox default user used to connect to the DB like anonymous user
			String username=BBConfiguration.getBaasBoxUsername();
			String password=BBConfiguration.getBaasBoxPassword();
			UserService.signUp(username, password,DefaultRoles.ANONYMOUS_USER.toString(), null,null,null,null);
	
			//the baasbox default user used to act internally as the administrator
			username=BBConfiguration.getBaasBoxAdminUsername();
			password=BBConfiguration.getBaasBoxAdminPassword();
			UserService.signUp(username, password,DefaultRoles.ADMIN.toString(), null,null,null,null);
			
			moveUserToRole("admin",DefaultRoles.BASE_ADMIN.toString(), DefaultRoles.ADMIN.toString());
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public static List<ODocument> getUsers(QueryParams criteria) throws SqlInjectionException{
		UserDao dao = UserDao.getInstance();
		return dao.get(criteria);
	}


	
	public static ODocument getCurrentUser() throws SqlInjectionException{
		return getUserProfilebyUsername(DbHelper.getCurrentUserName());
	}
	
	public static OUser getOUserByUsername(String username){
		return DbHelper.getConnection().getMetadata().getSecurity().getUser(username);	
	}
	
	public static ODocument getUserProfilebyUsername(String username) throws SqlInjectionException{
		UserDao dao = UserDao.getInstance();
		ODocument userDetails=null;
		userDetails=dao.getByUserName(username);
		return userDetails;
	}
	
	public static String getUsernameByProfile(ODocument profile) throws InvalidModelException{
		UserDao dao = UserDao.getInstance();
		dao.checkModelDocument(profile);
		return (String)((ODocument)profile.field("user")).field("name");
	}
	
	public static ODocument  signUp (
			String username,
			String password,
			JsonNode nonAppUserAttributes,
			JsonNode privateAttributes,
			JsonNode friendsAttributes,
			JsonNode appUsersAttributes) throws Exception{
		return signUp (
				username,
				password,
				null,
				nonAppUserAttributes,
				privateAttributes,
				friendsAttributes,
				appUsersAttributes) ;
	}
	
	public static void registerDevice(HashMap<String,Object> data) throws SqlInjectionException{
		ODocument user=getCurrentUser();
		ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
		ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
		String deviceId=(String) data.get(UserDao.USER_DEVICE_ID);
		boolean found=false;
		for (ODocument loginInfo : loginInfos){
			
			if (loginInfo.field(UserDao.USER_DEVICE_ID)!=null && loginInfo.field(UserDao.USER_DEVICE_ID).equals(deviceId)){
				found=true;
				break;
			}
		}
		if (!found){
			loginInfos.add(new ODocument(data));
			systemProps.save();
		}
	}
	
	public static void logout(String deviceId) throws SqlInjectionException {
		ODocument user=getCurrentUser();
		ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
		ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
		for (ODocument loginInfo : loginInfos){
			if (loginInfo.field(UserDao.USER_DEVICE_ID)!=null && loginInfo.field(UserDao.USER_DEVICE_ID).equals(deviceId)){
				loginInfos.remove(loginInfo);
				break;
			}
		}
		systemProps.save();
	}
	
	public static ODocument  signUp (
				String username,
				String password,
				String role,
				JsonNode nonAppUserAttributes,
				JsonNode privateAttributes,
				JsonNode friendsAttributes,
				JsonNode appUsersAttributes) throws Exception{
		
		
		OGraphDatabase db =  DbHelper.getConnection();
		ODocument profile=null;
		UserDao dao = UserDao.getInstance();
		try{
			//because we have to create an OUser record and a User Object, we need a transaction

			  DbHelper.requestTransaction();
			  
			  if (role==null) profile=dao.create(username, password);
			  else profile=dao.create(username, password,role);
			  
			  ORID userRid = ((ODocument)profile.field("user")).getIdentity();
			  ORole friendRole=RoleDao.createFriendRole(username);
			  friendRole.getDocument().field(RoleService.FIELD_ASSIGNABLE,true);
			  friendRole.getDocument().field(RoleService.FIELD_MODIFIABLE,false);
			  friendRole.getDocument().field(RoleService.FIELD_INTERNAL,true);
			  friendRole.getDocument().field(RoleService.FIELD_DESCRIPTION,"These are friends of " + username);
			  
			  /*    these attributes are visible by:
			   *    Anonymous users
			   *    Registered user
			   *    Friends
			   *    User
			   */
				if (nonAppUserAttributes!=null)  {
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					try{
						attrObj.fromJSON(nonAppUserAttributes.toString());
					}catch (OSerializationException e){
						throw new OSerializationException (dao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER + " is not a valid JSON object",e);
					}
					PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
					PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));	
					PermissionsHelper.grantRead(attrObj, friendRole);				
					PermissionsHelper.changeOwner(attrObj,userRid );
					profile.field(dao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER,attrObj);
					attrObj.save();
				}
				
				  /*    these attributes are visible by:
				   *    User
				   */				
				if (privateAttributes!=null) {
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					try{
						attrObj.fromJSON(privateAttributes.toString());
					}catch (OSerializationException e){
						throw new OSerializationException (dao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER + " is not a valid JSON object",e);
					}
					profile.field(dao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER, attrObj);
					PermissionsHelper.changeOwner(attrObj, userRid);					
					attrObj.save();
				}
				
				  /*    these attributes are visible by:
				   *    Friends
				   *    User
				   */				
				if (friendsAttributes!=null) {
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					try{	
						attrObj.fromJSON(friendsAttributes.toString());
					}catch (OSerializationException e){
						throw new OSerializationException (dao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER + " is not a valid JSON object",e);
					}
					PermissionsHelper.grantRead(attrObj, friendRole);				
					PermissionsHelper.changeOwner(attrObj, userRid);
					profile.field(dao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER, attrObj);
					attrObj.save();
				}
				
				  /*    these attributes are visible by:
				   *    Registered user
				   *    Friends
				   *    User
				   */				
				if (appUsersAttributes!=null) {
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					try{
						attrObj.fromJSON(appUsersAttributes.toString());
					}catch (OSerializationException e){
						throw new OSerializationException (dao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER + " is not a valid JSON object",e);
					}
					PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
					PermissionsHelper.grantRead(attrObj, friendRole);	
					PermissionsHelper.changeOwner(attrObj, userRid);
					profile.field(dao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER, attrObj);
					attrObj.save();
				}
				  
				ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
				attrObj.field(dao.USER_LOGIN_INFO, new ArrayList() );
				PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
				PermissionsHelper.changeOwner(attrObj, userRid);
				profile.field(dao.ATTRIBUTES_SYSTEM, attrObj);
				
				PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
				PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));
				PermissionsHelper.changeOwner(profile, userRid);
				
				profile.field(dao.USER_SIGNUP_DATE, new Date());
				profile.save();
			  
			  DbHelper.commitTransaction();
			}catch( Exception e ){
			 DbHelper.rollbackTransaction();
			  throw e;
			} 
		return profile;
	} //signUp

	public static ODocument updateProfile(ODocument profile, JsonNode nonAppUserAttributes,
			JsonNode privateAttributes, JsonNode friendsAttributes,
			JsonNode appUsersAttributes) throws Exception{
		if (nonAppUserAttributes!=null)  {
			ODocument attrObj = profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
			if (attrObj==null) attrObj=new ODocument(UserDao.USER_ATTRIBUTES_CLASS);
			attrObj.fromJSON(nonAppUserAttributes.toString());
			PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
			PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));	
			PermissionsHelper.grantRead(attrObj, RoleDao.getFriendRole());				
			profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER,attrObj);
			attrObj.save();
		}
		if (privateAttributes!=null)  {
			ODocument attrObj = profile.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
			if (attrObj==null) attrObj=new ODocument(UserDao.USER_ATTRIBUTES_CLASS);
			attrObj.fromJSON(privateAttributes.toString());
			PermissionsHelper.grant(attrObj, Permissions.ALLOW,getOUserByUsername(getUsernameByProfile(profile)));
			profile.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER, attrObj);
			attrObj.save();
		}
		if (friendsAttributes!=null)  {
			ODocument attrObj = profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
			if (attrObj==null) attrObj=new ODocument(UserDao.USER_ATTRIBUTES_CLASS);
			attrObj.fromJSON(friendsAttributes.toString());
			PermissionsHelper.grantRead(attrObj, RoleDao.getFriendRole());				
			profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER, attrObj);
			attrObj.save();
		}
		if (appUsersAttributes!=null)  {
			ODocument attrObj = profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
			if (attrObj==null) attrObj=new ODocument(UserDao.USER_ATTRIBUTES_CLASS);
			attrObj.fromJSON(appUsersAttributes.toString());
			PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
			PermissionsHelper.grantRead(attrObj, RoleDao.getFriendRole());	
			profile.field(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER, attrObj);
			attrObj.save();
		}
	  
		profile.save();
		return profile;
	}
	
	public static ODocument updateCurrentProfile(JsonNode nonAppUserAttributes,
			JsonNode privateAttributes, JsonNode friendsAttributes,
			JsonNode appUsersAttributes) throws Exception{
		try{
			ODocument profile = UserService.getCurrentUser();
			profile = updateProfile(profile, nonAppUserAttributes, privateAttributes, friendsAttributes, appUsersAttributes);
			return profile;
		}catch (Exception e){
			throw e;
		}
	}//update profile
	
	public static ODocument updateProfile(String username,String role,JsonNode nonAppUserAttributes,
			JsonNode privateAttributes, JsonNode friendsAttributes,
			JsonNode appUsersAttributes) throws Exception{
		try{
			ORole newORole=RoleDao.getRole(role);
			if (newORole==null) throw new InvalidParameterException(role + " is not a role");
			ORID newRole=newORole.getDocument().getIdentity();
			UserDao udao=UserDao.getInstance();
			ODocument profile=udao.getByUserName(username);
			if (profile==null) throw new InvalidParameterException(username + " is not a user");
			profile=updateProfile(profile, nonAppUserAttributes,
					 privateAttributes,  friendsAttributes, appUsersAttributes);

		    Set<OIdentifiable>roles=( Set<OIdentifiable>)((ODocument)profile.field("user")).field("roles");
		    //extracts the role skipping the friends ones
		    String oldRole=null;
		    for(OIdentifiable r:roles){
		    	oldRole=((String)((ODocument)r.getRecord()).field("name"));
		    	if (! oldRole.startsWith(RoleDao.FRIENDS_OF_ROLE)) {
		    		break;
		    	}
		    }
		    //TODO: update role
		   // OUser ouser=DbHelper.getConnection().getMetadata().getSecurity().getUser(username);
		   // ouser.removeRole(oldRole);
		    //ouser.addRole(newORole);
		    //ouser.save();
		    profile.save();
		    profile.reload();
			return profile;
		}catch (Exception e){
			throw e;
		}
	}//updateProfile with role

	public static void changePasswordCurrentUser(String newPassword) {
		OGraphDatabase db = DbHelper.getConnection();
		String username=db.getUser().getName();
		db = DbHelper.sudo();
		db.getMetadata().getSecurity().getUser(username).setPassword(newPassword).save();
		//DbHelper.removeConnectionFromPool();
	}
	
	public static boolean exists(String username) {
		UserDao udao=UserDao.getInstance();
		return udao.existsUserName(username);
	}
	

	public static void sendResetPwdMail(String appCode, ODocument user) throws Exception {
		final String errorString ="Cannot send mail to reset the password: ";

			//check method input
			if (!user.getSchemaClass().getName().equalsIgnoreCase(UserDao.MODEL_NAME)) throw new Exception (errorString + " invalid user object");

			//initialization
			String siteUrl = Application.NETWORK_HTTP_URL.getValueAsString();
			int sitePort = Application.NETWORK_HTTP_PORT.getValueAsInteger();
			if (StringUtils.isEmpty(siteUrl)) throw  new Exception (errorString + " invalid site url (is empty)");
			
			String textEmail = PasswordRecovery.EMAIL_TEMPLATE_TEXT.getValueAsString();
			String htmlEmail = PasswordRecovery.EMAIL_TEMPLATE_HTML.getValueAsString();
			if (StringUtils.isEmpty(htmlEmail)) htmlEmail=textEmail;
			if (StringUtils.isEmpty(htmlEmail)) throw  new Exception (errorString + " text to send is not configured");
			
			boolean useSSL = PasswordRecovery.NETWORK_SMTP_SSL.getValueAsBoolean();
			boolean useTLS = PasswordRecovery.NETWORK_SMTP_TLS.getValueAsBoolean();
			String smtpHost = PasswordRecovery.NETWORK_SMTP_HOST.getValueAsString();
			int smtpPort = PasswordRecovery.NETWORK_SMTP_PORT.getValueAsInteger();
			if (StringUtils.isEmpty(smtpHost)) throw  new Exception (errorString + " SMTP host is not configured");
			
			
			String username_smtp = null;
			String password_smtp = null;
			if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
				username_smtp = PasswordRecovery.NETWORK_SMTP_USER.getValueAsString();
				password_smtp = PasswordRecovery.NETWORK_SMTP_PASSWORD.getValueAsString();
				if (StringUtils.isEmpty(username_smtp)) throw  new Exception (errorString + " SMTP username is not configured");
			}
			String emailFrom = PasswordRecovery.EMAIL_FROM.getValueAsString();
			String emailSubject = PasswordRecovery.EMAIL_SUBJECT.getValueAsString();
			if (StringUtils.isEmpty(emailFrom)) throw  new Exception (errorString + " sender email is not configured");
			
			try {
				String userEmail=((ODocument) user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER)).field("email").toString();
				
				String username = (String) ((ODocument) user.field("user")).field("name");
				
				//Random
				String sRandom = appCode + "%%%%" + username + "%%%%" + UUID.randomUUID();
				String sBase64Random = new String(Base64.encodeBase64(sRandom.getBytes()));
				
				//Send mail
				HtmlEmail email = null;
				
				URL resetUrl = new URL(Application.NETWORK_HTTP_SSL.getValueAsBoolean()? "https" : "http", siteUrl, sitePort, "/user/password/reset/"+sBase64Random); 
				
				//HTML Email Text
				ST htmlMailTemplate = new ST(htmlEmail, '$', '$');
				htmlMailTemplate.add("link", resetUrl);
				htmlMailTemplate.add("user_name", username);
	
				//Plain text Email Text
				ST textMailTemplate = new ST(textEmail, '$', '$');
				textMailTemplate.add("link", resetUrl);
				textMailTemplate.add("user_name", username);
				
				email = new HtmlEmail();
	
				email.setHtmlMsg(htmlMailTemplate.render());
				email.setTextMsg(textMailTemplate.render());
				
				//Email Configuration
				email.setSSLOnConnect(useSSL);
				email.setStartTLSEnabled(useTLS);
				email.setHostName(smtpHost);
				email.setSmtpPort(smtpPort);
				
				if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
					email.setAuthenticator(new DefaultAuthenticator(username_smtp, password_smtp));
				}
				email.setFrom(emailFrom);			
				email.addTo(userEmail);
				
				email.setSubject(emailSubject);
				email.send();
				
				//Save on DB
				ResetPwdDao.getInstance().create(new Date(), sBase64Random, user);
			
		}  catch (EmailException authEx){
			throw new Exception (errorString + " Could not reach the mail server. Please contact the server administrator");
		}
			catch (Exception e) {
			throw new Exception (errorString,e);
		}
		
		
	}

	public static void resetUserPasswordFinalStep(String username, String newPassword) throws SqlInjectionException, ResetPasswordException {
		ODocument user = UserDao.getInstance().getByUserName(username);
		ODocument ouser = ((ODocument) user.field("user"));
		ouser.field("password",newPassword).save();
		ResetPwdDao.getInstance().setResetPasswordDone(username);
	}

	public static void moveUsersToRole(String from, String to) {
		String sqlAdd="update ouser add roles = {TO_ROLE} where roles contains {FROM_ROLE}";
		String sqlRemove="update ouser remove roles = {FROM_ROLE} where roles contains {FROM_ROLE}";
		ORole fromRole=RoleDao.getRole(from);
		ORole toRole=RoleDao.getRole(to);
		
		ORID fromRID=fromRole.getDocument().getRecord().getIdentity();
		ORID toRID=toRole.getDocument().getRecord().getIdentity();
		
		sqlAdd=sqlAdd.replace("{TO_ROLE}", toRID.toString()).replace("{FROM_ROLE}", fromRID.toString());
		sqlRemove=sqlRemove.replace("{TO_ROLE}", toRID.toString()).replace("{FROM_ROLE}", fromRID.toString());
		
		GenericDao.getInstance().executeCommand(sqlAdd, new String[] {});
		GenericDao.getInstance().executeCommand(sqlRemove, new String[] {});
	}
	
	public static void moveUserToRole(String username,String from, String to) {
		String sqlAdd="update ouser add roles = {TO_ROLE} where roles contains {FROM_ROLE} and name = ?";
		String sqlRemove="update ouser remove roles = {FROM_ROLE} where roles contains {FROM_ROLE} and name = ?";
		
		ORole fromRole=RoleDao.getRole(from);
		ORole toRole=RoleDao.getRole(to);
		
		ORID fromRID=fromRole.getDocument().getRecord().getIdentity();
		ORID toRID=toRole.getDocument().getRecord().getIdentity();
		
		sqlAdd=sqlAdd.replace("{TO_ROLE}", toRID.toString()).replace("{FROM_ROLE}", fromRID.toString());
		sqlRemove=sqlRemove.replace("{TO_ROLE}", toRID.toString()).replace("{FROM_ROLE}", fromRID.toString());

		GenericDao.getInstance().executeCommand(sqlAdd, new String[] {username});
		GenericDao.getInstance().executeCommand(sqlRemove, new String[] {username});
	}
	


}
