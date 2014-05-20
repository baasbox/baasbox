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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import play.Logger;

import com.baasbox.configuration.Internal;
import com.baasbox.dao.IndexDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabase.ATTRIBUTES;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Evolves the DB to the 0.7.3 schema
 * introducing the "File"s
 * 
 * @author Claudio Tesoriero
 *
 */
public class Evolution_0_7_4 implements IEvolution {
	private String version="0.7.4-snapshot";
	
	public Evolution_0_7_4() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			changePushTokenFieldName(db);
			addProfileSections(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void changePushTokenFieldName(ODatabaseRecordTx db)  {
		Logger.info("..changing 'deviceId' to 'pushToken' field name..:");
		UserDao dao = UserDao.getInstance();
		QueryParams criteria = QueryParams.getInstance();
		try {
			List<ODocument> users = UserService.getUsers(criteria);
			for (ODocument user:users){
				ODocument userSystemProperties = user.field(UserDao.ATTRIBUTES_SYSTEM);
				if (userSystemProperties!=null){
					List<ODocument> loginInfos=userSystemProperties.field(UserDao.USER_LOGIN_INFO);
					for(ODocument loginInfo : loginInfos){
						String deviceId=loginInfo.field("deviceId");
						loginInfo.field(UserDao.USER_PUSH_TOKEN, deviceId);
						loginInfo.save();
					}
					userSystemProperties.save();
				}
				user.save();
			}
		} catch (SqlInjectionException e) {
			throw new RuntimeException(e);
		}
		Logger.info("...done...");
	}
		
	private void addProfileSections(ODatabaseRecordTx db) {
		Logger.info("...adding missing profile section..:");
		UserDao dao = UserDao.getInstance();
		QueryParams criteria = QueryParams.getInstance();
		try {
			List<ODocument> users = UserService.getUsers(criteria);
			Logger.info(" found " + users.size() + " users");
			for (ODocument user:users){
			    ORID userRid = ((ODocument)user.field("user")).getIdentity();
				ODocument anonymousSection = user.field(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER);
				ODocument registeredSection = user.field(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER);
				ODocument privateSection = user.field(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER);
				ODocument friendsSection = user.field(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER);
				
				if (anonymousSection==null){
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					attrObj.fromJSON("{}");
                    PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()));        
                    PermissionsHelper.changeOwner(attrObj,userRid );
                    user.field(dao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER,attrObj);
                    attrObj.save();
				}
				
				if (registeredSection==null){
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					attrObj.fromJSON("{}");
					PermissionsHelper.grantRead(attrObj, RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()));       
	                PermissionsHelper.changeOwner(attrObj, userRid);
	                user.field(dao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER, attrObj);
                    attrObj.save();
				}

				if (privateSection==null){
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					attrObj.fromJSON("{}");
                    user.field(dao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER, attrObj);
                    PermissionsHelper.changeOwner(attrObj, userRid);                                        
                    attrObj.save();
				}				
				
				if (friendsSection==null){
					ODocument attrObj = new ODocument(dao.USER_ATTRIBUTES_CLASS);
					attrObj.fromJSON("{}");
					PermissionsHelper.grantRead(attrObj, RoleDao.getFriendRole(user));                                
				    PermissionsHelper.changeOwner(attrObj, userRid);
				    user.field(dao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER, attrObj);
				    attrObj.save();
				}
				
			}
		}catch (SqlInjectionException e) {
			throw new RuntimeException(e);
		}
		Logger.info("...done...");
	}



}
