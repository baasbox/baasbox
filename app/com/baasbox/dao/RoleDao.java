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



import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class RoleDao {
		public static final String 	FRIENDS_OF_ROLE = "friends_of_";
	
		
		public static ORole getRole(String name){
			OGraphDatabase db = DbHelper.getConnection();
			return db.getMetadata().getSecurity().getRole(name);
		}
		
		public static ORole createRole(String name){
			OGraphDatabase db = DbHelper.getConnection();
			final ORole role =  db.getMetadata().getSecurity().createRole(name, ORole.ALLOW_MODES.DENY_ALL_BUT);
				role.addRule(ODatabaseSecurityResources.DATABASE, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.SCHEMA, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.CLUSTER + "." + OMetadata.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.CLUSTER + ".orole", ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.CLUSTER + ".ouser", ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.ALL_CLASSES, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);
				role.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_READ);
			      
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
			OGraphDatabase db = DbHelper.getConnection();
			return getFriendRoleName(db.getUser());
		}
		
		public static ORole getFriendRole(){
			return getRole(getFriendRoleName());
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
			return createRole(getFriendRoleName(username));
		}
		
		
}
