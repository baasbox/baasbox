package com.baasbox.db;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import play.Logger;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

public class Evolutions {
	private  TreeMap<String,IEvolution> me = new TreeMap<String,IEvolution>();
	
	/***
	 * Executes db evolutions from the given version (excluded) that is supposed to be the current one
	 * @param fromVersion
	 */
	public static void performEvolutions(ODatabaseRecordTx db,String fromVersion){
		Evolutions evs=new Evolutions();
		Collection<IEvolution> evolutions = evs.getEvolutionsFromVersion(fromVersion);
		Logger.info("Found " + evolutions.size() + " evolutions to apply");
		Iterator<IEvolution> it = evolutions.iterator();
		while (it.hasNext()){
			IEvolution ev = it.next();
			Logger.info("Applying evolution to " + ev.getFinalVersion());
			ev.evolve(db);
		}
	}
	
	public Evolutions(){
		IEvolution ev= (IEvolution)new Evolution_0_7_0();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_7_3();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_7_4();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_7_5();
		me.put(ev.getFinalVersion(), ev);
	}
	
	public Collection<IEvolution> getEvolutions(){
		return me.values();
	}
	
	public Collection<IEvolution> getEvolutionsFromVersion(String fromVersion){
		NavigableMap<String, IEvolution> evolutions = me.tailMap(fromVersion, false);
		return evolutions.values();
	}
}
