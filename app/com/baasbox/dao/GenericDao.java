/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.dao;

import java.util.List;
import java.util.UUID;

import play.Logger;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OSQLHelper;


public class GenericDao {
	public static GenericDao getInstance(){
		return new GenericDao();
	}
	
	private GenericDao(){}
	
	public ODocument get(String rid) throws IllegalArgumentException{
		Object orid=OSQLHelper.parseValue(rid, null);
		if (!(orid instanceof ORecordId)) throw new IllegalArgumentException(rid +" is not a valid rid");
		ODocument doc=get((ORecordId)orid);
		return doc;
	}
	
	public ODocument get(ORID rid) {
		OGraphDatabase db =DbHelper.getConnection();
		Logger.trace("Method Start");
		ODocument doc=db.load(rid);
		Logger.trace("Method End");
		return doc;
	}
	
	public ORID getRidByUUID(UUID id){
		OGraphDatabase db =DbHelper.getConnection();
		OIndex<?> index = db.getMetadata().getIndexManager().getIndex("_BB_Node.id");
		ORID rid = (ORID) index.get(id.toString());
		return rid;
	}
	
	public ORID getRidByUUID(String id){
		  UUID uuid=UUID.fromString(id);
		  return getRidByUUID(uuid);
	}
	
	public List<ODocument>executeQuery(String oclass, QueryParams criteria) throws SqlInjectionException{
		OCommandRequest command = DbHelper.selectCommandBuilder(oclass, false, criteria);
		List<ODocument> result = DbHelper.commandExecute(command, criteria.getParams());
		return result;
	}
	


	public void executeCommand(String commandString, Object[] params) {
		OGraphDatabase db =  DbHelper.getConnection();
		OCommandRequest command=db.command(new OCommandSQL(commandString));
		command.execute(params);
	}
	
}
