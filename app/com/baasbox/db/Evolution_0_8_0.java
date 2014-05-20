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
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Evolves the DB to the 0.8.0 schema
 * 
 * @author Claudio Tesoriero
 *
 */
public class Evolution_0_8_0 implements IEvolution {
	private String version="0.8.0";
	
	public Evolution_0_8_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			setGraphDefaultValues(db);
            addPermissionsClass(db);
            idOnEdgeClass(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void setGraphDefaultValues(ODatabaseRecordTx db) {
		Logger.info("..updating graph custom attributes..:");
//		String[] script=new String[]{
//				"alter database custom useLightweightEdges=true;",
//				"alter database custom useClassForEdgeLabel=true",
//				"alter database custom useClassForVertexLabel=true",
//				"alter database custom useVertexFieldsForEdgeLabels=true"};
//		for (String line:script){
//			Logger.debug(line);
//			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
//				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
//			}
//		}
        DbHelper.execMultiLineCommands(db,true,
                "alter database custom useLightweightEdges=false;",
                "alter database custom useClassForEdgeLabel=false",
                "alter database custom useClassForVertexLabel=true",
                "alter database custom useVertexFieldsForEdgeLabels=true");
		Logger.info("...done...");
	}

    private void addPermissionsClass(ODatabaseRecordTx db) {
        Logger.info("..creating database permissions class...:");
        DbHelper.execMultiLineCommands(db,true,
            "create class _BB_Permissions;",
            "create property _BB_Permissions.tag String;",
            "create property _BB_Permissions.enabled boolean;",
            "alter property _BB_Permissions.tag mandatory=true;",
            "alter property _BB_Permissions.tag notnull=true;",
            "alter property _BB_Permissions.enabled mandatory=true;",
            "alter property _BB_Permissions.enabled notnull=true;",

            "create index _BB_Permissions.tag unique;"
        );
        DbHelper.createDefaultPermissionTags();
        Logger.info("...done...");
    }
    
    
    private void idOnEdgeClass(ODatabaseRecordTx db) {
        Logger.info("..creating id property on E class...:");
        DbHelper.execMultiLineCommands(db,true,
        		"create property E.id String;",
        		"alter property E.id notnull=true;",
        		"create index E.id unique;"
        );
        Logger.info("...done...");
    }
   
    
}
