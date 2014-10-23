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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.sf.ehcache.search.expression.Criteria;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.stringtemplate.v4.ST;

import play.Logger;

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
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.PasswordRecoveryException;
import com.baasbox.exception.RoleIsNotAssignableException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.sociallogin.UserInfo;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class UserService {

	public static void createDefaultUsers(){
		try{
			//the baasbox default user used to connect to the DB like anonymous user
			String username=BBConfiguration.getBaasBoxUsername();
			String password=BBConfiguration.getBaasBoxPassword();
			UserService.signUp(username, password,new Date(),DefaultRoles.ANONYMOUS_USER.toString(), null,null,null,null,false);
	
			//the baasbox default user used to act internally as the administrator
			username=BBConfiguration.getBaasBoxAdminUsername();
			password=BBConfiguration.getBaasBoxAdminPassword();
			UserService.signUp(username, password,new Date(),DefaultRoles.ADMIN.toString(), null,null,null,null,false);
			
			moveUserToRole("admin",DefaultRoles.BASE_ADMIN.toString(), DefaultRoles.ADMIN.toString());
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

    public static List<ODocument> getUsers(QueryParams criteria,boolean excludeInternal) throws SqlInjectionException {
        if (excludeInternal) {
            String where="user.name not in ?" ;
            if (!StringUtils.isEmpty(criteria.getWhere())) {
                where += " and (" + criteria.getWhere() + ")";
            }
            Object[] params = criteria.getParams();
            Object[] injectedParams = new String[]{BBConfiguration.getBaasBoxAdminUsername(),
                                                   BBConfiguration.getBaasBoxUsername()};
            Object[] newParams = ArrayUtils.addAll(new Object[]{injectedParams},params);
            criteria.where(where);
            criteria.params(newParams);
        }
        return getUsers(criteria);
    }


	public static List<ODocument> getUsers(QueryParams criteria) throws SqlInjectionException{
		UserDao dao = UserDao.getInstance();
		return dao.get(criteria);
	}



	
	public static ODocument getCurrentUser() throws SqlInjectionException{
		return getUserProfilebyUsername(DbHelper.getCurrentUserNameFromConnection());
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
			Date signupDate,
			JsonNode nonAppUserAttributes,
			JsonNode privateAttributes,
			JsonNode friendsAttributes,
			JsonNode appUsersAttributes,
			boolean generated) throws InvalidJsonException, UserAlreadyExistsException{
		return signUp (
				username,
				password,
				signupDate,
				null,
				nonAppUserAttributes,
				privateAttributes,
				friendsAttributes,
				appUsersAttributes,
				generated) ;
	}

	public static void registerDevice(HashMap<String,Object> data) throws SqlInjectionException{
		ODocument user=getCurrentUser();
		ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
		ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
		String pushToken=(String) data.get(UserDao.USER_PUSH_TOKEN);
		boolean found=false;
		for (ODocument loginInfo : loginInfos){

			if (loginInfo.field(UserDao.USER_PUSH_TOKEN)!=null && loginInfo.field(UserDao.USER_PUSH_TOKEN).equals(pushToken)){
				found=true;
				break;
			}
		}
		if (!found){
			loginInfos.add(new ODocument(data));
			systemProps.save();
		}
	}
	public static void unregisterDevice(String pushToken) throws SqlInjectionException{
		ODocument user=getCurrentUser();
		ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
		ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
		for (ODocument loginInfo : loginInfos){
			if (loginInfo.field(UserDao.USER_PUSH_TOKEN)!=null && loginInfo.field(UserDao.USER_PUSH_TOKEN).equals(pushToken)){
				loginInfos.remove(loginInfo);
				break;
			}
		}
		systemProps.save();
	}
	
	

	public static void logout(String pushToken) throws SqlInjectionException {
		ODocument user=getCurrentUser();
		ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
		ArrayList<ODocument> loginInfos=systemProps.field(UserDao.USER_LOGIN_INFO);
		for (ODocument loginInfo : loginInfos){
			if (loginInfo.field(UserDao.USER_PUSH_TOKEN)!=null && loginInfo.field(UserDao.USER_PUSH_TOKEN).equals(pushToken)){
				loginInfos.remove(loginInfo);
				break;
			}
		}
		systemProps.save();
	}

	public static ODocument  signUp (
            String username,
            String password,
            Date signupDate,
            String role,
            JsonNode nonAppUserAttributes,
            JsonNode privateAttributes,
            JsonNode friendsAttributes,
            JsonNode appUsersAttributes,boolean generated) throws InvalidJsonException,UserAlreadyExistsException{
			
			if (StringUtils.isEmpty(username)) throw new IllegalArgumentException("username cannot be null or empty");
			if (StringUtils.isEmpty(password)) throw new IllegalArgumentException("password cannot be null or empty");
			
			ODatabaseRecordTx db =  DbHelper.getConnection();
			ODocument profile=null;
			UserDao dao = UserDao.getInstance();
			try{
			    //because we have to create an OUser record and a User Object, we need a transaction
			
			      DbHelper.requestTransaction();
			      
			      if (role==null) profile=dao.create(username, password);
			      else profile=dao.create(username, password,role);
			      
			      ORID userRid = ((ORID)profile.field("user")).getIdentity();
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
			      
			      //anonymous
			           {
			                    ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
			                    try{
			                    	  if (nonAppUserAttributes!=null) attrObj.fromJSON(nonAppUserAttributes.toString());
			                    	  else attrObj.fromJSON("{}");
			                    }catch (OSerializationException e){
			                            throw new InvalidJsonException (dao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER + " is not a valid JSON object",e);
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
			            {
			                    ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
			                    try{
			                    	if (privateAttributes!=null) attrObj.fromJSON(privateAttributes.toString());
			                    	else attrObj.fromJSON("{}");
			                    }catch (OSerializationException e){
			                            throw new InvalidJsonException (dao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER + " is not a valid JSON object",e);
			                    }
			                    profile.field(dao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER, attrObj);
			                    PermissionsHelper.changeOwner(attrObj, userRid);                                        
			                    attrObj.save();
			            }
			            
			              /*    these attributes are visible by:
			               *    Friends
			               *    User
			               */                                
			           {
			                    ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
			                    try{        
			                    	 if (friendsAttributes!=null) attrObj.fromJSON(friendsAttributes.toString());
			                     	else attrObj.fromJSON("{}");
			                    }catch (OSerializationException e){
			                            throw new InvalidJsonException (dao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER + " is not a valid JSON object",e);
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
			           {
			                    ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
			                    try{
			                    	if (appUsersAttributes!=null) attrObj.fromJSON(appUsersAttributes.toString());
			                     	else attrObj.fromJSON("{}");
			                    }catch (OSerializationException e){
			                            throw new InvalidJsonException (dao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER + " is not a valid JSON object",e);
			                    }
			                    attrObj.field("_social",new HashMap());
			                    PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));       
			                    PermissionsHelper.changeOwner(attrObj, userRid);
			                    profile.field(dao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER, attrObj);
			                    attrObj.save();
			            }
			              
			            //system info
			            {
				            ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
				            attrObj.field(dao.USER_LOGIN_INFO, new ArrayList() );
				            attrObj.field(dao.USER_SIGNUP_DATE, signupDate==null?new Date():signupDate);
				            PermissionsHelper.changeOwner(attrObj, userRid);
				            profile.field(dao.ATTRIBUTES_SYSTEM, attrObj);   
			            }
			            
			            profile.field(dao.USER_SIGNUP_DATE, signupDate==null?new Date():signupDate);
			            //this is useful when you want to know if the username was automatically generated
			            profile.field(UserDao.GENERATED_USERNAME,generated);
			            
			            PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
			            PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));
			            PermissionsHelper.changeOwner(profile, userRid);
			            
			            
			            profile.save();
			            DbHelper.commitTransaction();
				}catch( OSerializationException e ){
				    DbHelper.rollbackTransaction();
				    throw new InvalidJsonException(e);
				}catch( InvalidJsonException e ){
				    DbHelper.rollbackTransaction();
				    throw e;
				}catch( UserAlreadyExistsException e ){
				    DbHelper.rollbackTransaction();
				    throw e;
			    }catch( Exception e ){
			     DbHelper.rollbackTransaction();
			      throw new RuntimeException(ExceptionUtils.getStackTrace(e));
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
			//preserve the _social field
				OTrackedMap oldSocial = (OTrackedMap)attrObj.field("_social");
				((ObjectNode)(appUsersAttributes)).remove("_social");
			attrObj.fromJSON(appUsersAttributes.toString());
				if (oldSocial!=null) attrObj.field("_social",oldSocial);
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
			JsonNode appUsersAttributes) throws InvalidJsonException,Exception{
		try{
			ORole newORole=RoleDao.getRole(role);
			if (newORole==null) throw new InvalidParameterException(role + " is not a role");
			if (!RoleService.isAssignable(newORole)) throw new RoleIsNotAssignableException("Role " + role + " is not assignable");
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
			ORole oldORole=RoleDao.getRole(oldRole);
			//TODO: update role
			OUser ouser=DbHelper.getConnection().getMetadata().getSecurity().getUser(username);
			ouser.getRoles().remove(oldORole);
			ouser.addRole(newORole);
			ouser.save();
			profile.save();
			profile.reload();

			return profile;
		}catch (OSerializationException e){
			throw new InvalidJsonException(e);
		}catch (Exception e){
			throw e;
		}
	}//updateProfile with role

	public static void changePasswordCurrentUser(String newPassword) throws OpenTransactionException {
		ODatabaseRecordTx db = DbHelper.getConnection();
		String username=db.getUser().getName();
		db = DbHelper.reconnectAsAdmin();
		db.getMetadata().getSecurity().getUser(username).setPassword(newPassword).save();
		//DbHelper.removeConnectionFromPool();
	}
	
	public static void changePassword(String username, String newPassword) throws SqlInjectionException, UserNotFoundException, OpenTransactionException {
		ODatabaseRecordTx db=DbHelper.getConnection();
		db = DbHelper.reconnectAsAdmin();
		UserDao udao=UserDao.getInstance();
		ODocument user = udao.getByUserName(username);
		if(user==null){
			if (Logger.isDebugEnabled()) Logger.debug("User " + username + " does not exist");
			throw new UserNotFoundException("User " + username + " does not exist");
		}
		db.getMetadata().getSecurity().getUser(username).setPassword(newPassword).save();
	}

	public static boolean exists(String username) {
		UserDao udao=UserDao.getInstance();
		return udao.existsUserName(username);
	}


	public static void sendResetPwdMail(String appCode, ODocument user) throws Exception {
		final String errorString ="Cannot send mail to reset the password: ";

		//check method input
		if (!user.getSchemaClass().getName().equalsIgnoreCase(UserDao.MODEL_NAME)) throw new PasswordRecoveryException (errorString + " invalid user object");

		//initialization
		String siteUrl = Application.NETWORK_HTTP_URL.getValueAsString();
		int sitePort = Application.NETWORK_HTTP_PORT.getValueAsInteger();
		if (StringUtils.isEmpty(siteUrl)) throw  new PasswordRecoveryException (errorString + " invalid site url (is empty)");

		String textEmail = PasswordRecovery.EMAIL_TEMPLATE_TEXT.getValueAsString();
		String htmlEmail = PasswordRecovery.EMAIL_TEMPLATE_HTML.getValueAsString();
		if (StringUtils.isEmpty(htmlEmail)) htmlEmail=textEmail;
		if (StringUtils.isEmpty(htmlEmail)) throw  new PasswordRecoveryException (errorString + " text to send is not configured");

		boolean useSSL = PasswordRecovery.NETWORK_SMTP_SSL.getValueAsBoolean();
		boolean useTLS = PasswordRecovery.NETWORK_SMTP_TLS.getValueAsBoolean();
		String smtpHost = PasswordRecovery.NETWORK_SMTP_HOST.getValueAsString();
		int smtpPort = PasswordRecovery.NETWORK_SMTP_PORT.getValueAsInteger();
		if (StringUtils.isEmpty(smtpHost)) throw  new PasswordRecoveryException (errorString + " SMTP host is not configured");


		String username_smtp = null;
		String password_smtp = null;
		if (PasswordRecovery.NETWORK_SMTP_AUTHENTICATION.getValueAsBoolean()) {
			username_smtp = PasswordRecovery.NETWORK_SMTP_USER.getValueAsString();
			password_smtp = PasswordRecovery.NETWORK_SMTP_PASSWORD.getValueAsString();
			if (StringUtils.isEmpty(username_smtp)) throw  new PasswordRecoveryException (errorString + " SMTP username is not configured");
		}
		String emailFrom = PasswordRecovery.EMAIL_FROM.getValueAsString();
		String emailSubject = PasswordRecovery.EMAIL_SUBJECT.getValueAsString();
		if (StringUtils.isEmpty(emailFrom)) throw  new PasswordRecoveryException (errorString + " sender email is not configured");

		try {
			String userEmail=((ODocument) user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER)).field("email").toString();

			String username = (String) ((ODocument) user.field("user")).field("name");

			//Random
			String sRandom = appCode + "%%%%" + username + "%%%%" + UUID.randomUUID();
			String sBase64Random = new String(Base64.encodeBase64(sRandom.getBytes()));

			//Save on DB
			ResetPwdDao.getInstance().create(new Date(), sBase64Random, user);

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
				Logger.debug("Password Recovery is ready to send: \n" + logEmail.toString());
			}
			email.send();

		}  catch (EmailException authEx){
			Logger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(authEx));
			throw new PasswordRecoveryException (errorString + " Could not reach the mail server. Please contact the server administrator");
		}  catch (Exception e) {
			Logger.error("ERROR SENDING MAIL:" + ExceptionUtils.getStackTrace(e));
			throw new Exception (errorString,e);
		}


	}

	public static void resetUserPasswordFinalStep(String username, String newPassword) throws SqlInjectionException, ResetPasswordException {
		ODocument user = UserDao.getInstance().getByUserName(username);
		ODocument ouser = ((ODocument) user.field("user"));
		ouser.field("password",newPassword).save();
		ResetPwdDao.getInstance().setResetPasswordDone(username);
	}



	public static void removeSocialLoginTokens(ODocument user , String socialNetwork) throws ODatabaseException{
		DbHelper.requestTransaction();
		try{
			ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
			Map<String,ODocument>  ssoTokens = systemProps.field(UserDao.SOCIAL_LOGIN_INFO);
			if(ssoTokens == null){
				throw new ODatabaseException(socialNetwork + " is not linked with this account");
			}else{
				ssoTokens.remove(socialNetwork);
				systemProps.field(UserDao.SOCIAL_LOGIN_INFO,ssoTokens);
				user.field(UserDao.ATTRIBUTES_SYSTEM,systemProps);
				systemProps.save();
				user.save();
				if (Logger.isDebugEnabled()) Logger.debug("saved tokens for user ");
				DbHelper.commitTransaction();
			}
		}catch(Exception e){
			e.printStackTrace();
			DbHelper.rollbackTransaction();
			throw new ODatabaseException("unable to add tokens");
		}

	}

	public static void addSocialLoginTokens(ODocument user , UserInfo userInfo) throws ODatabaseException {
		DbHelper.requestTransaction();
		try{
			ODocument systemProps=user.field(UserDao.ATTRIBUTES_SYSTEM);
			Map<String,ODocument>  ssoTokens = systemProps.field(UserDao.SOCIAL_LOGIN_INFO);
			if(ssoTokens == null){
				ssoTokens = new HashMap<String,ODocument>();
			}

			String jsonRep = userInfo.toJson();
			ssoTokens.put(userInfo.getFrom(), (ODocument)new ODocument().fromJSON(jsonRep));
			systemProps.field(UserDao.SOCIAL_LOGIN_INFO,ssoTokens);
			user.field(UserDao.ATTRIBUTES_SYSTEM,systemProps);
			systemProps.save();
			
			ODocument registeredUserProp = user.field(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
			Map socialdata=registeredUserProp.field("_social");
			if(socialdata == null){
				socialdata = new HashMap<String,ODocument>();
			}
			socialdata.put(userInfo.getFrom(), (ODocument)new ODocument().fromJSON("{\"id\":\""+userInfo.getId()+"\"}"));
			registeredUserProp.field("_social",socialdata);
			registeredUserProp.save();
			user.save();
			if (Logger.isDebugEnabled()) Logger.debug("saved tokens for user ");
			DbHelper.commitTransaction();

		}catch(Exception e){
			DbHelper.rollbackTransaction();
			throw new ODatabaseException("unable to add tokens");
		}

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
	
	public static void addUserToRole(String username,String role) throws OpenTransactionException{
		boolean admin = true;
		if(!DbHelper.currentUsername().equals(BBConfiguration.getBaasBoxAdminUsername())){
			DbHelper.reconnectAsAdmin();
			admin = false;
		}
		String sqlAdd="update ouser add roles = {TO_ROLE} where name = ?";
		ORole toRole=RoleDao.getRole(role);
		ORID toRID=toRole.getDocument().getRecord().getIdentity();
		sqlAdd=sqlAdd.replace("{TO_ROLE}", toRID.toString());
		GenericDao.getInstance().executeCommand(sqlAdd, new String[] {username});
		if(!admin){
			DbHelper.reconnectAsAuthenticatedUser();
		}
		
	}
	
	public static void removeUserFromRole(String username,String role) throws OpenTransactionException{
		boolean admin = false;
		if(!DbHelper.currentUsername().equals(BBConfiguration.getBaasBoxAdminUsername())){
			DbHelper.reconnectAsAdmin();
			admin = true;
		}
		String sqlRemove="update ouser remove roles = {FROM_ROLE} where roles contains {FROM_ROLE} and name = ?";
		ORole fromRole=RoleDao.getRole(role);
		ORID fromRID=fromRole.getDocument().getRecord().getIdentity();
		sqlRemove=sqlRemove.replace("{FROM_ROLE}", fromRID.toString());
		GenericDao.getInstance().executeCommand(sqlRemove, new String[] {username});
		if(admin){
			DbHelper.reconnectAsAuthenticatedUser();
		}
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
	
	
	
	public static void disableUser(String username) throws UserNotFoundException, OpenTransactionException{
		UserDao.getInstance().disableUser(username);
	}

	public static void disableCurrentUser() throws UserNotFoundException, OpenTransactionException{
		String username = DbHelper.currentUsername();
		disableUser(username);
	}
	
	public static void enableUser(String username) throws UserNotFoundException, OpenTransactionException{
		UserDao.getInstance().enableUser(username);
	}

    public static List<ODocument> getUserProfileByUsernames(List<String> usernames,QueryParams criteria) throws SqlInjectionException {
        return UserDao.getInstance().getByUsernames(usernames,criteria);
    }

	public static List<ODocument> getUserProfilebyUsernames(List<String> usernames) throws SqlInjectionException {
		return UserDao.getInstance().getByUsernames(usernames);
		
	}
	
	public static boolean userCanByPassRestrictedAccess(String userName){
		ORole role = getUserRole(userName);
		return RoleService.roleCanByPassRestrictedAccess(role.getName());
	}

	public static ORole getUserRole(String username){
		OUser ouser = getOUserByUsername(username);
		for (ORole r: ouser.getRoles()){
			if (!r.getName().startsWith(FriendShipService.FRIEND_ROLE_NAME)) return r;
		}
		return null;
	}

    public static boolean isInternalUsername(String username) {
        return BBConfiguration.getBaasBoxAdminUsername().equals(username)||
               BBConfiguration.getBaasBoxUsername().equals(username);
    }
}
