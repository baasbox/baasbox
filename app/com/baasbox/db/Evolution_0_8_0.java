package com.baasbox.db;

import play.Logger;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Evolves the DB to the 0.7.5 schema
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
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	private void setGraphDefaultValues(ODatabaseRecordTx db) {
		Logger.info("..updating graph custom attributes..:");
		String[] script=new String[]{
				"alter database custom useLightweightEdges=true;",
				"alter database custom useClassForEdgeLabel=true",
				"alter database custom useClassForVertexLabel=true",
				"alter database custom useVertexFieldsForEdgeLabels=true"};
		for (String line:script){
			Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Logger.info("...done...");
	}

}
