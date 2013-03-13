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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import play.Logger;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;



public class PermissionsHelper {
	public enum Permissions {
		ALLOW ("_allow"),
	    ALLOW_READ ("_allowRead"),
	    ALLOW_UPDATE ("_allowUpdate"),
	    ALLOW_DELETE ("_allowDelete"),
		FULL_ACCESS ("_allow");
	    
	    private Permissions(String name)
	    {
	        this.name = name;
	    }
	    
	    public String toString()
	    {
	        return name;
	    }
	    
	    private String name;
	    
	}
	
	public static final Map<String, Permissions> permissionsFromString = ImmutableMap.of(
	        "read", Permissions.ALLOW_READ,
	        "update", Permissions.ALLOW_UPDATE,
	        "delete",Permissions.ALLOW_DELETE,
	        "all",Permissions.ALLOW
	); 
	
	public static ODocument grantRead(ODocument document,ORole role){
		return grant(document,Permissions.ALLOW_READ,role);	
	}
	
	public static ODocument grantRead(ODocument document,OUser user){
		return grant(document,Permissions.ALLOW_READ,user);		
	}
	
	public static ODocument changeOwner(ODocument document,OUser user){
		Logger.trace("Method Start");
		Set<ORID> set = new HashSet<ORID>();
		set.add( user.getDocument().getIdentity() ); 
		document.field( Permissions.ALLOW.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document;		
	}	
	
	public static ODocument changeOwner(ODocument document,OIdentifiable user){
		Logger.trace("Method Start");
		Set<OIdentifiable> set = new HashSet<OIdentifiable>();
		set.add( user ); 
		document.field( Permissions.ALLOW.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document;		
	}

	/* Delayed due an OrientDb bug, see patch below
	public static ODocument grant(ODocument document, Permissions permission,
			ORole role) {
		Logger.trace("Method Start");
		if (role==null){
			Logger.warn("role is null! Grant command skipped");
			return document;
		}
		OGraphDatabase db = DbHelper.getConnection();
		db.getMetadata().getSecurity().allowRole(document, permission.toString(), role.getName());
		document.save(); 
		Logger.trace("Method End");
		return document;
	}
	
	public static ODocument grant(ODocument document, Permissions permission,
			OUser user) {
		Logger.trace("Method Start");
		if (user==null){
			Logger.warn("user is null! Grant command skipped");
			return document;
		}		
		OGraphDatabase db = DbHelper.getConnection();
		db.getMetadata().getSecurity().allowUser(document, permission.toString(), user.getName());
		Logger.trace("Method End");
		return document;
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			ORole role) {
		Logger.trace("Method Start");
		if (role==null){
			Logger.warn("role is null! Revoke command skipped");
			return document;
		}
		OGraphDatabase db = DbHelper.getConnection();
		db.getMetadata().getSecurity().disallowRole(document, permission.toString(), role.getName());
		document.save(); 
		Logger.trace("Method End");
		return document;
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			OUser user) {
		Logger.trace("Method Start");
		if (user==null){
			Logger.warn("user is null! Revoke command skipped");
			return document;
		}		
		OGraphDatabase db = DbHelper.getConnection();
		db.getMetadata().getSecurity().disallowUser(document, permission.toString(), user.getName());
		Logger.trace("Method End");
		return document;
	}
*/
	
	public static ODocument grant(ODocument document, Permissions permission,
			ORole role) {
		Logger.trace("Method Start");
		if (role==null){
			Logger.warn("role is null! Grant command skipped");
			return document;
		}
		Set<ORID> set = document.field(  permission.toString(), OType.LINKSET ); 
		if (set==null) set = new HashSet<ORID>();
		set.add( role.getDocument().getIdentity() ); 
		document.field( permission.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document;
	}
	
	public static ODocument grant(ODocument document, Permissions permission,
			OUser user) {
		Logger.trace("Method Start");
		Set<ORID> set = document.field(  permission.toString(), OType.LINKSET ); 
		if (set==null) set = new HashSet<ORID>();
		set.add( user.getDocument().getIdentity() ); 
		document.field( permission.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document; 
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			ORole role) {
		Logger.trace("Method Start");
		Set<ORID> set = document.field(  permission.toString(), OType.LINKSET ); 
		if (set==null) return document;
		set.remove( role.getDocument().getIdentity() ); 
		document.field( permission.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document;
	}
	
	public static ODocument revoke(ODocument document, Permissions permission,
			OUser user) {
		Logger.trace("Method Start");
		Set<ORID> set = document.field(  permission.toString(), OType.LINKSET ); 
		if (set==null) return document;
		set.remove( user.getDocument().getIdentity() ); 
		document.field( permission.toString(), set, OType.LINKSET ); 
		document.save(); 
		Logger.trace("Method End");
		return document;
	}
}
