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
package com.baasbox.dao;



import java.util.List;
import java.util.Map;

import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.OMetadataDefault;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class RoleDao {
		/**
		 * Role associated to the friends of a given user
		 */
		public static final String 	FRIENDS_OF_ROLE = "friends_of_";
		/**
		 * Field of a role that state the inherited role.
		 */
		public static final String 	FIELD_INHERITED = "inheritedRole";
		

		public static final String  READER_BASE_ROLE = "reader";
		public static final String  WRITER_BASE_ROLE = "writer";
		public static final String  ADMIN_BASE_ROLE = "admin";
		
		public static ORole getRole(String name){
			ODatabaseRecordTx db = DbHelper.getConnection();
            return db.getMetadata().getSecurity().getRole(name);
        }

		public static ORole createRole(String name,String inheritedRoleName){
			ODatabaseRecordTx db = DbHelper.getConnection();
			ORole inheritedRole = db.getMetadata().getSecurity().getRole(inheritedRoleName);
			final ORole role =  db.getMetadata().getSecurity().createRole(name,inheritedRole.getMode());
			role.getDocument().field(FIELD_INHERITED,inheritedRole.getDocument().getRecord());
			role.getDocument().field("isrole",true);
			role.save();
	        return role;
		}
		
		public static ORole createRole(String name,ORole.ALLOW_MODES mode,Map rules){
			ODatabaseRecordTx db = DbHelper.getConnection();
			final ORole role =  db.getMetadata().getSecurity().createRole(name,mode);
			role.getDocument().field("rules",rules);
			role.getDocument().field("isrole",true);
			role.save();
	        return role;
		}
		


		
		public static String getFriendRoleName(String username){
			return FRIENDS_OF_ROLE + username;
		}
		
		public static String getFriendRoleName(OUser user){
			return getFriendRoleName(user.getName());
		}
		
		public static String getFriendRoleName(){
			ODatabaseRecordTx db = DbHelper.getConnection();
			return getFriendRoleName(db.getUser());
		}
		
		public static ORole getFriendRole(){
			return getRole(getFriendRoleName(DbHelper.currentUsername()));
		}
		public static ORole getFriendRole(String username){
			return getRole(getFriendRoleName(username));
		}
		public static ORole getFriendRole(OUser user){
			return getRole(getFriendRoleName(user));
		}
		public static ORole getFriendRole(ODocument profile){
			ODocument user = profile.field("user");
			String username = user.field("name");
			return getRole(getFriendRoleName(username));
		}
		
		public static ORole createFriendRole(String username){
			return createRole(getFriendRoleName(username),READER_BASE_ROLE);
		}

		public static boolean exists(String roleName) {
			return (DbHelper.getConnection().getMetadata().getSecurity().getRole(roleName)!=null);
		}

		public static void delete(String name) {
			DbHelper.getConnection().getMetadata().getSecurity().dropRole(name);
		}
		
		
}
