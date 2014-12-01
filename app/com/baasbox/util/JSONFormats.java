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

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;



public class JSONFormats {

	public enum Formats{
		USER("fetchPlan:user.password:-2 user.roles.name:1 user.roles.inheritedRole:-2 user.roles.rules:-2 user.roles:0 user.roles.mode:-2 user.roles.internal:-2 user.roles.modifiable:-2 user.roles.assignable:-2 user.roles.description:-2 _creation_date:-2 visibleByAnonymousUsers:1 visibleByTheUser:1 visibleByFriends:1 visibleByRegisteredUsers:1 _links:-2 _audit:-2 system:-2 _allow:-2 _allowRead:-2,indent:0"),
		JSON("fetchPlan:user.password:-2 user.status:-2 user.roles:-1 user.roles.mode:-2 user.roles.inheritedRole:-2 user.roles.name:1 user.roles.rules:-2 visibleByTheUser:1 visibleByFriends:1 visibleByRegisteredUsers:1 system:-2 _links:-2 _audit:-2 _allow:-2,indent:0"),
		DOCUMENT("fetchPlan:audit:0 _links:-2 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0,rid,version,class,attribSameRow,alwaysFetchEmbedded,indent:0"),
		DOCUMENT_WITH_ACL("fetchPlan:_allow:-2 password:-2 "
				+"_allowRead:1 _allowRead.name:1 _allowRead.isrole:1 _allowRead.@version:-2 _allowRead.inheritedRole:-2 _allowRead.mode:-2 _allowRead.rules:-2 _allowRead.internal:-2 _allowRead.modifiable:-2 _allowRead.description:-2 _allowRead.assignable:-2 _allowRead.roles:-2 _allowRead.status:-2 _allowRead.password:-2 "
				+"_allowUpdate:1 _allowUpdate.name:1 _allowUpdate.isrole:1 _allowUpdate.@version:-2 _allowUpdate.inheritedRole:-2 _allowUpdate.mode:-2 _allowUpdate.rules:-2 _allowUpdate.internal:-2 _allowUpdate.modifiable:-2 _allowUpdate.description:-2 _allowUpdate.assignable:-2 _allowUpdate.roles:-2 _allowUpdate.status:-2 _allowUpdate.password:-2 "
				+"_allowDelete:-2 _allowDelete.name:1 _allowDelete.isrole:1 _allowDelete.@version:-2 _allowDelete.inheritedRole:-2 _allowDelete.mode:-2 _allowDelete.rules:-2 _allowDelete.internal:-2 _allowDelete.modifiable:-2 _allowDelete.description:-2 _allowDelete.assignable:-2 _allowDelete.roles:-2 _allowDelete.status:-2 _allowDelete.password:-2"
				+",version,class,attribSameRow,alwaysFetchEmbedded,indent:0"),
				OBJECT("fetchPlan:audit:0 _links:0 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0,keepTypes,attribSameRow,alwaysFetchEmbedded,indent:0"),
		ASSET("fetchPlan:resized:-2 audit:0 _links:0 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0 file:0,rid,version,class,attribSameRow,indent:0"),
		//LINK("fetchPlan:*:0 out:-2 in:-2 from.data:1 to.data:1,version,class,attribSameRow,alwaysFetchEmbedded,indent:0"),
		LINK("fetchPlan:*:0 out.data:1 in.data:1,version,class,attribSameRow,alwaysFetchEmbedded,indent:0"),
		FILE("fetchPlan:resized:-2 audit:0 _links:0 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0 file:-2 metadata:1 text_content:-2,version,attribSameRow,indent:0"),
		ROLES("indent:0,fetchPlan:rules:-2 inheritedRole:-2"),
        DOCUMENT_PUBLIC("fetchPlan:_audit:-2 _links:-2 _allow:0 _allowread:0 _allowwrite:0 _allowUpdate:0 _allowDelete:0,rid,version,class,attribSameRow,alwaysFetchEmbedded,indent:0")
		//,STORE("fetchPlan:")
				;


		
		private String format;
		
		private Formats(String format){
			this.format=format;
		}
		
		public String toString(){
			return format;
		}
	}

	private static ODocument cutBaasBoxFields(ODocument doc, boolean preserveAcl){
		DocumentCutter cutter=new DocumentCutter(doc);
		ODocument ret = cutter.getCuttedDoc(preserveAcl);
		return ret;
	}

    public static String prepareDocToJson(ODocument doc,JSONFormats.Formats format){
        return doc.toJSON(format.toString());
    }

    public static String prepareDocToJson(List<ODocument> docs,JSONFormats.Formats format){
        return OJSONWriter.listToJSON(docs,format.toString());
    }


	public static String prepareResponseToJson(ODocument doc, JSONFormats.Formats format){
		return prepareResponseToJson(doc,format,false);
	}
	
	public static String prepareResponseToJson(ODocument doc, JSONFormats.Formats format,boolean preserveACl){
		 ODocument retDoc = JSONFormats.cutBaasBoxFields(doc,preserveACl);
		 String ret = retDoc.toJSON(format.toString());
		return ret;
	}
	
	public static String prepareResponseToJson(List<ODocument> listOfDoc,JSONFormats.Formats format) throws IOException{
		return prepareResponseToJson(listOfDoc,format,false);
	}
	
	public static String prepareResponseToJson(List<ODocument> listOfDoc,JSONFormats.Formats format,boolean preserveAcl) throws IOException{
		for (ODocument doc: listOfDoc){
			JSONFormats.cutBaasBoxFields(doc,preserveAcl);
		}
		return OJSONWriter.listToJSON(listOfDoc,format.toString());
	}
	
	
}
