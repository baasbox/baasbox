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
package com.baasbox.db.hook;

import play.Logger;

import com.orientechnologies.orient.core.hook.ORecordHookAbstract;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class HidePassword extends ORecordHookAbstract {

	public static HidePassword getIstance(){
		return new HidePassword();
	}

	@Override
	public void onRecordAfterRead(ORecord<?> iRecord) {
		Logger.trace("Method Start");
		super.onRecordAfterRead(iRecord);
		Logger.debug("HidePasswordHook.onRecordAfterRead");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
			if (doc.getClassName()!=null && doc.getClassName().equalsIgnoreCase("OUser")){
				Logger.debug ("  hiding password from user " + (String)doc.field("name"));
				doc.removeField("password");
			}
		}
		Logger.trace("Method End");
	}//onRecordAfterRead
}
