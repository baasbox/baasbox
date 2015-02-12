package com.baasbox.db;

import com.baasbox.service.permissions.PermissionTagService;
import com.baasbox.service.permissions.Tags;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import play.Logger;

/**
 * Created by eto on 10/23/14.
 */
public class Evolution_0_9_4 implements IEvolution {
    private String version = "0.9.4";

    @Override
    public String getFinalVersion() {
        return version;
    }

    @Override
    public void evolve(ODatabaseRecordTx db) {
        Logger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            createDescriptionsForEndpointSwitches(db);
        }catch (Throwable e){
            Logger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        Logger.info ("DB now is on " + version + " level");
    }


    private void createDescriptionsForEndpointSwitches(ODatabaseRecordTx db) {
    	Logger.info("Creating descriptions for endpoint switches...");
    	 DbHelper.execMultiLineCommands(db,true,
                 "update _BB_permissions set description='Access to APIs for reading and for asset downloading.' where tag='baasbox.assets';",
                 "update _BB_permissions set description='Allows users to access their accounts, login and logout, modify their passwords.' where tag='baasbox.account';",
                 "update _BB_permissions set description='Allows users to create new accounts (signup via username/password).' where tag='baasbox.account.create';",
                 "update _BB_permissions set description='Allows login/signup through supported social networks.' where tag='baasbox.social';",
                 "update _BB_permissions set description='Enables the workflow to reset a password via email.' where tag='baasbox.account.lost_password';",
                 "update _BB_permissions set description='Allows to query users\\' profiles.' where tag='baasbox.users';",
                 "update _BB_permissions set description='Allows to know your followers or the people you follow on the social platform within BaasBox.' where tag='baasbox.friendship';",
                 "update _BB_permissions set description='Enables the social functions of BaasBox: following/unfollowing among the users of the BaasBox server.' where tag='baasbox.friendship.create';",
                 "update _BB_permissions set description='Allows registered users to send push notifications to other users. Administrators can always send notifications to users.' where tag='baasbox.notifications.send';",
                 "update _BB_permissions set description='Allows apps to send device tokens they need in order to receive push notifications through BaasBox. If you disable these functions, apps wil no longer be able to register in order to receive push notifications from such server. Apps that are already registered will keep on receiving notifications.' where tag='baasbox.notifications.receive';",
                 "update _BB_permissions set description='Allows to create new Documents.' where tag='baasbox.data.write';",
                 "update _BB_permissions set description='Allows to read Collections and Documents by querying them.' where tag='baasbox.data.read';",
                 "update _BB_permissions set description='Allows to update the contents of already existing Documents.' where tag='baasbox.data.update';",
                 "update _BB_permissions set description='Enables the possibility to modify the ACL of Documents.' where tag='baasbox.data.grants';",
                 "update _BB_permissions set description='Allows to download the Files stored in BaasBox and query them.' where tag='baasbox.file.read';",
                 "update _BB_permissions set description=' Allows to create new Files.' where tag='baasbox.file.write';",
                 "update _BB_permissions set description='Enables the possibility to modify the ACL of Files.' where tag='baasbox.file.grants';",
                 "update _BB_permissions set description='Allows to make calls to plugins.' where tag='baasbox.scripts.invoke';"               
                 );
    	Logger.info("...done");
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
    
	private void createBBNodeIndexes(ODatabaseRecordTx db) {
		DbHelper.execMultiLineCommands(db, true, new String[]{
			"create property _BB_Node._author String;",
			"create index _bb_node._author notunique;",
			"create index _bb_node._creation_date notunique;"
		});
	}
}
