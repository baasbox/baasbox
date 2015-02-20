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

package com.baasbox.db;

import com.baasbox.service.logging.BaasBoxLogger;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;

public class Evolution_0_8_1 implements IEvolution {
	private String version="0.8.1";
	
	public Evolution_0_8_1() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		BaasBoxLogger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			setUsernameCaseInsensitive(db);
		}catch (Throwable e){
			BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		BaasBoxLogger.info ("DB now is on " + version + " level");
	}
	
	private void setUsernameCaseInsensitive(ODatabaseRecordTx db) {
		BaasBoxLogger.info("..updating ouser.name collate CI..:");
      		DbHelper.execMultiLineCommands(db,BaasBoxLogger.isDebugEnabled(),
      	            "drop index ouser.name;",
      	            "alter property ouser.name collate ci;",
      	            "create index ouser.name unique;"
      	        );
		BaasBoxLogger.info("...done...");
	}

    
    
}
