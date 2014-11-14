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

public enum BaasBoxPrivateFields {
	ID	("id",true),
	LINKS	("_links") ,
	AUDIT	("_audit") ,
	ALLOW	("_allow"),
	ALLOW_READ	("_allowRead"),
	ALLOW_WRITE	("_allowWrite"),
	ALLOW_UPDATE	("_allowUpdate"),
	ALLOW_DELETE	("_allowDelete"),
	CREATION_DATE	("_creation_date",true),
	AUTHOR			("_author",true),
	ACL				("_acl");
	private String field;
	private boolean visibleByTheClient=false;
	


	private BaasBoxPrivateFields(String field){
		this.field=field;
	}
	
	private BaasBoxPrivateFields(String field, boolean exportToClient){
		this.field=field;
		this.visibleByTheClient=exportToClient;
	}
	
	public String toString(){
		return field;
	}
	public boolean isVisibleByTheClient() {
		return visibleByTheClient;
	}
	public static String[] getFields(){
		ArrayList<String> fields=new ArrayList<String>();
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			fields.add(r.toString());
		}
		return (String[]) fields.toArray(new String[BaasBoxPrivateFields.values().length]);
	}
}
