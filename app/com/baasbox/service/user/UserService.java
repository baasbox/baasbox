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


import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import com.baasbox.dao.DefaultRoles;
import com.baasbox.dao.GenericDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.PermissionsHelper.Permissions;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;


public class UserService {


	public static List<ODocument> getUsers(QueryParams criteria) throws SqlInjectionException{
		UserDao dao = UserDao.getInstance();
		return dao.get(criteria);
	}

	public static List<ODocument> getRoles() throws SqlInjectionException {
		GenericDao dao = GenericDao.getInstance();
		QueryParams criteria = QueryParams.getInstance().where("name not like \""+RoleDao.FRIENDS_OF_ROLE+"%\"").orderBy("name asc");
		return dao.query("orole", criteria);
	}
	
	public static ODocument getCurrentUser() throws SqlInjectionException{
		UserDao dao = UserDao.getInstance();
		ODocument userDetails=null;
		userDetails=dao.getByUserName(DbHelper.getCurrentUserName());
		return userDetails;
	}
	
	public static OUser getOUserByUsername(String username){
		return DbHelper.getConnection().getMetadata().getSecurity().getUser(username);	
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

			  db.begin(TXTYPE.OPTIMISTIC);
			  
			  if (role==null) profile=dao.create(username, password);
			  else profile=dao.create(username, password,role);
			  
			  ORID userRid = ((ODocument)profile.field("user")).getIdentity();
			  ORole friendRole=RoleDao.createFriendRole(username);
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
				  
				PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));
				PermissionsHelper.grantRead(profile, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));
				PermissionsHelper.changeOwner(profile, userRid);
				
				profile.save();
			  
			  db.commit();
			}catch( Exception e ){
			 db.rollback();
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

	public static void changePassword(String newPassword) {
		OGraphDatabase db =  DbHelper.getConnection();
		db.getUser().setPassword(newPassword).save();
	}
	

}
