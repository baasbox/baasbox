package com.baasbox.db;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

public interface IEvolution {

	public  String getFinalVersion();
	public  void evolve (ODatabaseRecordTx db);

}
