package com.baasbox.db;

import com.orientechnologies.orient.core.db.graph.OGraphDatabase;

public interface IEvolution {

	public  String getFinalVersion();
	public  void evolve (OGraphDatabase db);

}
