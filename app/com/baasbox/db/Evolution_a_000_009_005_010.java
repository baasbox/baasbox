package com.baasbox.db;

import java.util.List;

import com.baasbox.service.logging.BaasBoxLogger;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseRecordWrapperAbstract;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClusters;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.OStorage;



/**
 * Created by eto on 10/23/14.
 */

/**
 * Issue 914 - Enormous default.pcl file size
 * https://github.com/baasbox/baasbox/issues/914
 * This evolution fixes the database removing old BLOBs associated to deleted file objects
 * @author giastfader
 *
 */
public class Evolution_a_000_009_005_010 implements IEvolution {
    private String version = "a.000.009.005.010";

    @Override
    public String getFinalVersion() {
        return version;
    }

    @Override
    public void evolve(ODatabaseRecordTx db) {
        BaasBoxLogger.info("Applying evolutions to evolve to the " + version + " level");
        try{
            removeUselessBlobs(db);
        }catch (Throwable e){
        	BaasBoxLogger.error("Error applying evolution to " + version + " level!!" ,e);
            throw new RuntimeException(e);
        }
        BaasBoxLogger.info ("DB now is on " + version + " level");
    }


    private void removeUselessBlobs(ODatabaseRecordTx db){
    	BaasBoxLogger.info("Starting patch for issue 914 (this may take some time, please be patient)...");
    	long start = System.currentTimeMillis();
    	OCommandRequest commandFile = db.command(
        		new OCommandSQL("select  from _bb_file where file = ?"));
        OCommandRequest commandAsset = db.command(
        		new OCommandSQL("select  from _bb_asset where file = ?"));
        ORecordIteratorClusters<ORecordInternal<?>> iterator = new ORecordIteratorClusters<ORecordInternal<?>>(db, db, new int[]  {3} , false, false, OStorage.LOCKING_STRATEGY.DEFAULT)
                .setRange(null,null); //{3} is the default.pcl cluster, where BLOBs are stored
        boolean toDelete=false;
        long totalSizeDeleted = 0;
        long totalCount = 0;
        while (iterator.hasNext()){
        	toDelete = false;
        	ORecordInternal<?> element = iterator.next();
        	String docType = new String(new byte[]{element.getRecordType()},0);
        	
        	if (docType.equals("b")){
        		List queryResultFile = (List)commandFile.execute(element.getIdentity());
        		List queryResultAsset = (List)commandAsset.execute(element.getIdentity());
        		if (queryResultFile.size()==0 && queryResultAsset.size()==0) {
        			toDelete = true;
        			totalSizeDeleted += element.getSize();
        			totalCount++;
        		}
        	}
        	BaasBoxLogger.info("...RID: " + element.getIdentity().toString() + " type: " + docType + " - " + " size: " + element.getSize() + " to delete: " + toDelete);
        	if (toDelete){
        		element.delete();
        	}
        }
        System.out.println("...Patching resized file references...");
        OCommandRequest commandResized = db.command(
        		new OCommandSQL("update _bb_file set resized = {}"));
        commandResized.execute();
        
        BaasBoxLogger.info("Total removed BLOB(s): " + totalCount);
        BaasBoxLogger.info("Total space freed (bytes): " +  totalSizeDeleted);
        BaasBoxLogger.info("Total execution time (ms): " + (System.currentTimeMillis()-start));

        
    	BaasBoxLogger.info("...done!");
    }

   
}
