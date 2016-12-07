package com.baasbox.db;

import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.permissions.PermissionTagService;
import com.baasbox.service.permissions.Tags;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

/**
 * Created by eto on 10/23/14.
 */
public class Evolution_0_9_0 implements IEvolution {
    private String version = "0.9.0";

    @Override
    public String getFinalVersion() {
        return version;
    }

    @Override
    public void evolve(ODatabaseRecordTx db) {
        BaasBoxLogger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            addScriptsClass(db);
            addScriptsPermission();
            addRoleFlag(db);
            createBBNodeIndexes(db);
        }catch (Throwable e){
            BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        BaasBoxLogger.info ("DB now is on " + version + " level");
    }


    private void addScriptsClass(ODatabaseRecordTx db){
        BaasBoxLogger.info("Creating scripts classes...");
        DbHelper.execMultiLineCommands(db,true,
                "create class _BB_Script;" ,
                "create property _BB_Script.name String;",
                "alter property _BB_Script.name mandatory=true;" ,
                "alter property _BB_Script.name notnull=true;" ,
                "create property _BB_Script.code embeddedlist string;" ,
                "alter property _BB_Script.code mandatory=true;" ,
                "alter property _BB_Script.code notnull=true;" ,
                "create property _BB_Script.lang String;" ,
                "alter property _BB_Script.lang mandatory=true;" ,
                "alter property _BB_Script.lang notnull=true;" ,
                "create property _BB_Script.library boolean;" ,
                "alter property _BB_Script.library mandatory=true;" ,
                "alter property _BB_Script.library notnull=true;" ,
                "create property _BB_Script.active boolean;" ,
                "alter property _BB_Script.active mandatory=true;" ,
                "alter property _BB_Script.active notnull=true;" ,
                "create property _BB_Script._storage embedded;" ,
                "create property _BB_Script._creation_date datetime;" ,
                "create property _BB_Script._invalid boolean;" ,
                "alter property _BB_Script._invalid mandatory=true;" ,
                "alter property _BB_Script._invalid notnull=true;" ,
                "create index _BB_Script.name unique;");
        BaasBoxLogger.info("...done!");
    }

    private void addScriptsPermission() {
        BaasBoxLogger.info("Creating scripts permission tag...");
        PermissionTagService.createReservedPermission(Tags.Reserved.SCRIPT_INVOKE);
        BaasBoxLogger.info("...done!");
    }
    
    private void addRoleFlag(ODatabaseRecordTx db) {
        BaasBoxLogger.info("Adding role flag on class OROLE...");
        DbHelper.execMultiLineCommands(db,true,false,
        		"create property orole.isrole boolean;",
        		"update orole set isrole=true");
        BaasBoxLogger.info("...done!");
    }
    
	private void createBBNodeIndexes(ODatabaseRecordTx db) {
		DbHelper.execMultiLineCommands(db, true, new String[]{
			"create property _BB_Node._author String;",
			"create index _bb_node._author notunique;",
			"create index _bb_node._creation_date notunique;"
		});
	}
}
