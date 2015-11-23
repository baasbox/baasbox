package com.baasbox.db;

import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;



/**
 * Created by eto on 10/23/14.
 */

/**
 * Removes the default OrientDB users reader and writer
 * @author giastfader
 *
 */
public class Evolution_a_000_009_005_001 implements IEvolution {
    private String version = "a.000.009.005.001";

    @Override
    public String getFinalVersion() {
        return version;
    }

    @Override
    public void evolve(ODatabaseRecordTx db) {
        BaasBoxLogger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            removeDefaultUser(db);
        }catch (Throwable e){
        	BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        BaasBoxLogger.info ("DB now is on " + version + " level");
    }


    private void removeDefaultUser(ODatabaseRecordTx db){
    	BaasBoxLogger.info("Changing indexes type...");
        DbHelper.execMultiLineCommands(db,true,false,
               "delete from OUser where name in ['reader','writer']"
        		);
        BaasBoxLogger.info("...done!");
    }

   
}
