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


import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;

import com.baasbox.service.logging.BaasBoxLogger;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.hook.ORecordHook.HOOK_POSITION;

public class HooksManager { 
	public static void registerAll(ODatabaseRecordTx db){

		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Registering hooks...");
		//we have to check if the hooks have been already registered since the connections could be reused due to pool 

		boolean register=true;
		//OrientDB 1.7: 
		Map<ORecordHook, HOOK_POSITION> hooks = db.getHooks();
		Iterator<ORecordHook> it =hooks.keySet().iterator();

		while (it.hasNext()){		
			if (it.next() instanceof BaasBoxHook) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("BaasBox hooks already registerd for this connection");
				register=false;
				break;
			}
		}
		if (register){
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Registering BaasBox hooks... start");
			db.registerHook(Audit.getIstance(),HOOK_POSITION.REGULAR);
			db.registerHook(HidePassword.getIstance(),HOOK_POSITION.LAST);
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Registering BaasBox hooks... done");
		}
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Hooks: "+ db.getHooks());
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
		
	}
	
	public static void unregisteredAll(ODatabaseRecordTx db){

		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
		
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("unregistering hooks...");
		//OrientDB 1.7: 
		Map<ORecordHook, HOOK_POSITION> hooks = db.getHooks();
		List hs = IteratorUtils.toList(hooks.keySet().iterator());
		Iterator<ORecordHook> it =hs.iterator();
		while (it.hasNext()){
			ORecordHook h = it.next();
			if (h instanceof BaasBoxHook) {
				if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Removing "+ ((BaasBoxHook) h).getHookName() + " hook");
				db.unregisterHook(h);
			}
		}
				
		if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
	}
	
	public static void enableHidePasswordHook(ODatabaseRecordTx db,boolean enable){
		Map<ORecordHook, HOOK_POSITION> hooks = db.getHooks();
		List hs = IteratorUtils.toList(hooks.keySet().iterator());
		Iterator<ORecordHook> it =hs.iterator();
		while (it.hasNext()){
			ORecordHook h = it.next();
			if (h instanceof HidePassword) {
				if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Enable: "+ enable+ " " + ((BaasBoxHook) h).getHookName() + " hook");
				((HidePassword) h).enable(enable);
				break;
			}
		}
	}
}
