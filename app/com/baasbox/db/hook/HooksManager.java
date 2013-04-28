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

import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.hook.ORecordHook.HOOK_POSITION;

public class HooksManager {
	public static void registerAll(OGraphDatabase db){
		Logger.trace("Method Start");
		db.registerHook(Audit.getIstance(),HOOK_POSITION.REGULAR);
		//db.registerHook(HidePassword.getIstance());
		Logger.trace("Method End");
	}
}
