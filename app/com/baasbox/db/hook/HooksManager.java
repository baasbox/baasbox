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
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;

import play.Logger;

import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.hook.ORecordHook.HOOK_POSITION;

public class HooksManager { 
	public static void registerAll(OGraphDatabase db){
		Logger.trace("Method Start");
		Logger.debug("Registering hooks...");
		//we have to check if the hooks have been already registered since the connections could be reused due to pool 
		Set<ORecordHook> hooks = db.getHooks();
		Iterator<ORecordHook> it =hooks.iterator();
		boolean register=true;
		while (it.hasNext()){		
			if (it.next() instanceof BaasBoxHook) {
				Logger.debug("BaasBox hooks already registerd for this connection");
				register=false;
				break;
			}
		}
		if (register){
			Logger.debug("Registering BaasBox hooks... start");
			db.registerHook(Audit.getIstance(),HOOK_POSITION.REGULAR);
			Logger.debug("Registering BaasBox hooks... done");
		}
		Logger.debug("Hooks: "+ db.getHooks());
		Logger.trace("Method End");
	}
	
	public static void unregisteredAll(OGraphDatabase db){

		Logger.trace("Method Start");
		
		Logger.debug("unregistering hooks...");
		Set<ORecordHook> hooks = db.getHooks();
		List hs = IteratorUtils.toList(hooks.iterator());
		Iterator<ORecordHook> it =hs.iterator();
		while (it.hasNext()){
			ORecordHook h = it.next();
			if (h instanceof BaasBoxHook) {
				Logger.debug("Removing "+ ((BaasBoxHook) h).getHookName() + " hook");
				db.unregisterHook(h);
			}
		}
				
		Logger.trace("Method End");

	}
}
