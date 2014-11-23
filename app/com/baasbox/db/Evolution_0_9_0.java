package com.baasbox.db;

import com.baasbox.service.permissions.PermissionTagService;
import com.baasbox.service.permissions.Tags;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import play.Logger;

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
        Logger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            addScriptsClass(db);
            addScriptsPermission();
            addRoleFlag(db);
        }catch (Throwable e){
            Logger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        Logger.info ("DB now is on " + version + " level");
    }


    private void addScriptsClass(ODatabaseRecordTx db){
        Logger.info("Creating scripts classes...");
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
        Logger.info("...done!");
    }

    private void addScriptsPermission() {
        Logger.info("Creating scripts permission tag...");
        PermissionTagService.createReservedPermission(Tags.Reserved.SCRIPT_INVOKE);
        Logger.info("...done!");
    }
    
    private void addRoleFlag(ODatabaseRecordTx db) {
        Logger.info("Adding role flag on class OROLE...");
        DbHelper.execMultiLineCommands(db,true,
        		"create property orole.isrole boolean;",
        		"update orole set isrole=true");
        Logger.info("...done!");
    }
}
