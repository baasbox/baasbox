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
package com.baasbox.enumerations;

import java.util.ArrayList;

import com.baasbox.dao.RoleDao;
import com.orientechnologies.orient.core.metadata.security.ORole;

public enum DefaultRoles {
	ADMIN			("admin") ,
	REGISTERED_USER	("registereduser") ,
	ANONYMOUS_USER	("anonymoususer"),
	BACKOFFICE_USER	("backofficeuser"); 
	
	private String role;
	private ORole orole;
	
	private DefaultRoles(String role){
		this.role=role;
	}
	
	public String toString(){
		return role;
	}
	
	public ORole getORole(){
		if (orole==null) orole=RoleDao.getRole(role);
		return orole;
	}
	
	public static ORole[] getORoles(){
		ArrayList<ORole> oroles=new ArrayList<ORole>();
		for (DefaultRoles r : DefaultRoles.values()){
			oroles.add(r.getORole());
		}
		return (ORole[]) oroles.toArray(new ORole[DefaultRoles.values().length]);
	}

}
