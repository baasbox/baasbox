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

import play.Logger;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

public class Evolution_0_8_3 implements IEvolution {
	private String version="0.8.3";
	
	public Evolution_0_8_3() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			setIndexOnUsername(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void setIndexOnUsername(ODatabaseRecordTx db) {
		Logger.info("..creating index on _bb_user.user.name..:");
      		DbHelper.execMultiLineCommands(db,Logger.isDebugEnabled(),
      	            "create index _bb_user.user.name unique;"
      	        );
		Logger.info("...done...");
	}

    
    
}