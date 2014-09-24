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
package com.baasbox.db.hook;

import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class HidePassword extends BaasBoxHook {
	private boolean enable=false;
	
	public static HidePassword getIstance(){
		return new HidePassword();
	}
	
	protected HidePassword() {
		super();
	}
	
	@Override
	 public  void onRecordAfterRead(ORecord<?> doc){
		if (!enable) return;
		if (doc instanceof ODocument){
			if ("OUser".equalsIgnoreCase(((ODocument)doc).getClassName())) {
				((ODocument) doc).removeField("password");
			}
		}
	 }//onRecordAfterRead

	public void enable(boolean enable){
		this.enable=enable;
	}

	@Override
	public String getHookName() {
		return "HidePassword";
	}
}
