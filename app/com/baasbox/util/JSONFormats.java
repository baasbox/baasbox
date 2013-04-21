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
package com.baasbox.util;

import java.io.IOException;
import java.util.List;

import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;



public class JSONFormats {
	
	public enum Formats{
		USER("fetchPlan:user.password:-2 user.roles.name:1 user.roles.rules:-2 user.roles:0 visibleByAnonymousUsers:1 visibleByTheUser:1 visibleByFriend:1 visibleByRegisteredUsers:1,indent:0"),
		DOCUMENT("fetchPlan:audit:0 _links:0 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0,rid,version,class,keepTypes,attribSameRow,alwaysFetchEmbedded,indent:0"),
		ASSET("fetchPlan:resized:-2 audit:0 _links:0 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0 file:0,rid,version,class,keepTypes,attribSameRow,indent:0"),
		ROLES("rid,indent:0,fetchPlan:rules:0");
		
		private String format;
		
		private Formats(String format){
			this.format=format;
		}
		
		public String toString(){
			return format;
		}
	}

	public static ODocument cutBaasBoxFields(ODocument doc){
		for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
			doc.removeField(r.toString());
		}
		return doc;
	}
	
	public static String prepareResponseToJson(ODocument doc, JSONFormats.Formats format){
		return JSONFormats.cutBaasBoxFields(doc).toJSON(format.toString());
	}
	
	public static String prepareResponseToJson(List<ODocument> listOfDoc,JSONFormats.Formats format) throws IOException{
		for (ODocument doc: listOfDoc){
			JSONFormats.cutBaasBoxFields(doc);
		}
		return OJSONWriter.listToJSON(listOfDoc,format.toString());
	}
	
}
