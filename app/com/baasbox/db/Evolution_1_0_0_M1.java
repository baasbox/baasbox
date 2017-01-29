package com.baasbox.db;

import java.util.List;

import play.Logger;

import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Created by eto on 10/23/14.
 */
public class Evolution_1_0_0_M1 implements IEvolution {
    private String version = "1.0.0-M1";

    @Override
    public String getFinalVersion() {
        return version;
    }

    @Override
    public void evolve(ODatabaseRecordTx db) {
        Logger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            changeIndexesType(db);
            //createCollectionIndexes(db);  //postponed
        }catch (Throwable e){
            Logger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        Logger.info ("DB now is on " + version + " level");
    }


    private void changeIndexesType(ODatabaseRecordTx db){
        Logger.info("Changing indexes type...");
        DbHelper.execMultiLineCommands(db,true,
               "drop index _BB_Collection.name;",
               "drop index _BB_Node.id;",
               "drop index _BB_Permissions.tag;",
               "drop index E.id;",
               "create index _BB_Collection.name UNIQUE_HASH_INDEX;",
               "create index _BB_Node.id UNIQUE_HASH_INDEX;",
               "create index _BB_Permissions.tag UNIQUE_HASH_INDEX;",
               "create index E.id UNIQUE_HASH_INDEX;"
        		);
        Logger.info("...done!");
    }

    /* TODO: postponed */
    private void createCollectionIndexes(ODatabaseRecordTx db) throws SqlInjectionException{
        Logger.info("Creating collection indexes...");
        Logger.info("...dropping old _BB_NODE indexes");
        DbHelper.execMultiLineCommands(db,true,
        		"drop index _bb_node._author;",
        		"drop index _bb_node._creation_date;");
        List<ODocument> collections = DbHelper.commandExecute(DbHelper.selectCommandBuilder("_BB_COLLECTION", false, QueryParams.getInstance()), 
        		new String[]{});
        Logger.info("...found " + collections.size() + " collections");
        for (ODocument coll:collections){
        	DbHelper.execMultiLineCommands(db,true,
            		"create index " + coll.field("name") + "._author notunique;",
            		"create index " + coll.field("name") + "._creation_date notunique;"
            		);
        	
        }
        Logger.info("...done!");
    }
}
