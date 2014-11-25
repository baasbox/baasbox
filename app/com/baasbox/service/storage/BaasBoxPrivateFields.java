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
package com.baasbox.service.storage;

import java.util.ArrayList;

import com.baasbox.enumerations.Permissions;

public enum BaasBoxPrivateFields {
	ID	("id",true,false),
	LINKS	("_links") ,
	AUDIT	("_audit") ,
	ALLOW	("_allow"),
	ALLOW_READ	(Permissions.ALLOW_READ.toString(),false,true),
	ALLOW_UPDATE	(Permissions.ALLOW_UPDATE.toString(),false,true),
	ALLOW_DELETE	(Permissions.ALLOW_DELETE.toString(),false,true),
	CREATION_DATE	("_creation_date",true,false),
	AUTHOR			("_author",true,false);
	
	private String field;
	private boolean visibleByTheClient=false;
	private boolean aclField=false;


	private BaasBoxPrivateFields(String field){
		this.field=field;
	}
	
	private BaasBoxPrivateFields(String field, boolean exportToClient,boolean isAclField){
		this.field=field;
		this.visibleByTheClient=exportToClient;
		this.aclField=isAclField;
	}
	
	public String toString(){
		return field;
	}
	public boolean isVisibleByTheClient() {
		return visibleByTheClient;
	}
	public boolean isAclField() {
		return aclField;
	}
	
	public static String[] getFields(){
		ArrayList<String> fields=new ArrayList<String>();
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			fields.add(r.toString());
		}
		return (String[]) fields.toArray(new String[BaasBoxPrivateFields.values().length]);
	}
}
