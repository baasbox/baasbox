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

import java.util.Date;
import java.util.List;

import play.Logger;

import com.baasbox.BBInternalConstants;
import com.baasbox.db.hook.HooksManager;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Evolution_0_9_0 implements IEvolution {
	private String version="0.9.0";
	
	public Evolution_0_9_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			updateLastUpdateDate(db);
			createDeleteClass(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void createDeleteClass(ODatabaseRecordTx db) {
		DbHelper.execMultiLineCommands(db,true,"create class _BB_Deleted extends ORestricted;",
				"create property _BB_Deleted.id String;",
				"create index _BB_Deleted.id unique;");
	}

	private void updateLastUpdateDate(ODatabaseRecordTx db) {
		Logger.info("..update _update_date..:");
		//deactivate HOOKS
		HooksManager.unregisteredAll(db);
		//retrieve all records belonging to the _BB_NODE OrientDB class
		OCommandRequest command = DbHelper.genericSQLStatementCommandBuilder("select count(*) as count from _BB_Node");
		List<ODocument> listOfDocs = DbHelper.selectCommandExecute(command, new Object[]{});
		Long docSize=listOfDocs.get(0).field("count");
		Logger.info("...about to update " + docSize + " records...");
		
		command = DbHelper.genericSQLStatementCommandBuilder("select from _BB_Node");
		listOfDocs = DbHelper.selectCommandExecute(command, new Object[]{});
		//for each record missing the _update_date field
		for (ODocument doc: listOfDocs){
			//add the _update_date using the _audit._last_update_date field
				ODocument audit = doc.field(BBInternalConstants.FIELD_AUDIT);
				if (audit!=null){
					doc.field("_update_date",(Date)audit.field("modifiedOn"));
				}else{
					//if for some reason the _audit info don't exist, we assume that the _update_date is equals to the _creation_date
					doc.field("_update_date",(Date)doc.field("_creation_date"));
				}
				doc.save();
		}
		//reactivate HOOKS
		HooksManager.registerAll(db);
		Logger.info("...done...");
	}

    
    
}
