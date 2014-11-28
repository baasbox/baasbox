/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
		preEvolutionTasks(db);
		Evolutions evs=new Evolutions();
		Collection<IEvolution> evolutions = evs.getEvolutionsFromVersion(fromVersion);
		Logger.info("Found " + evolutions.size() + " evolutions to apply");
		Iterator<IEvolution> it = evolutions.iterator();
		while (it.hasNext()){
			IEvolution ev = it.next();
			Logger.info("Applying evolution to " + ev.getFinalVersion());
			ev.evolve(db);
		}
		postEvolutionTasks(db);
	}
	
	private static void  preEvolutionTasks(ODatabaseRecordTx db){
		Logger.info("Performing pre-evolutions tasks....");
		//nothing todo at the moment
		Logger.info("...end");
	}
	
	private static void  postEvolutionTasks(ODatabaseRecordTx db){
		Logger.info("Performing post-evolutions tasks....");
		//nothing todo here at the moment
		Logger.info("...end");
	}
	
	public Evolutions(){
		IEvolution ev= (IEvolution)new Evolution_0_7_0();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_7_3();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_7_4();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_8_0();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_8_1();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_8_3();
		me.put(ev.getFinalVersion(), ev);
		ev= (IEvolution)new Evolution_0_8_4();
		me.put(ev.getFinalVersion(), ev);
		ev = (IEvolution)new Evolution_0_9_0();
		me.put(ev.getFinalVersion(),ev);
	}
	
	public Collection<IEvolution> getEvolutions(){
		return me.values();
	}
	
	public Collection<IEvolution> getEvolutionsFromVersion(String fromVersion){
		NavigableMap<String, IEvolution> evolutions = me.tailMap(fromVersion, false);
		return evolutions.values();
	}
}
