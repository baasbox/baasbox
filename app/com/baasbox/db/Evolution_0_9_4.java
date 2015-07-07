package com.baasbox.db;

import java.util.UUID;

import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

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
        BaasBoxLogger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            createDescriptionsForEndpointSwitches(db);
            createIndexOnEmail(db);
            insertIdforUsers(db);
        }catch (Throwable e){
            BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        BaasBoxLogger.info ("DB now is on " + version + " level");
    }


    private void insertIdforUsers(ODatabaseRecordTx db) {
    	BaasBoxLogger.info("Generating IDs for users ...");
    	// UUID() function is not available in OrientDB 1.7.10, so we have to define a one of ours
    	OSQLEngine.getInstance().registerFunction("bb_uuid",
    	                                          new OSQLFunctionAbstract("bb_uuid", 0, 0) {
    	  public String getSyntax() {
    	    return "bb_uuid()";
    	  }
    	  public boolean aggregateResults() {
    	    return false;
    	  }
		@Override
		public Object execute(Object iThis, OIdentifiable iCurrentRecord,
				Object iCurrentResult, Object[] iParams,
				OCommandContext iContext) {
			return UUID.randomUUID().toString();
		}
    	});
    	 DbHelper.execMultiLineCommands(db,true,
                 "update _BB_User set id=bb_uuid();");
    	 OSQLEngine.getInstance().unregisterFunction("bb_uuid");
    	BaasBoxLogger.info("...done");
	}

	private void createIndexOnEmail(ODatabaseRecordTx db) {
       	BaasBoxLogger.info("Creating index on email attribute...");
       	DbHelper.execMultiLineCommands(db,true,
       			"create property _bb_userattributes.email string;",
       			"create index _bb_userattributes.email notunique;"
                );
       	BaasBoxLogger.info("...done");
    }

	private void createDescriptionsForEndpointSwitches(ODatabaseRecordTx db) {
    	BaasBoxLogger.info("Creating descriptions for endpoint switches...");
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
    	BaasBoxLogger.info("...done");
	}

}
