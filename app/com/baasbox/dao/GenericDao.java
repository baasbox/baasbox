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

import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.OSQLHelper;


public class GenericDao {
	public static GenericDao getInstance(){
		return new GenericDao();
	}
	
	private GenericDao(){}
	
	/***
	 * Get a DB element by its RID
	 * @param rid the RID in format #xx:yy
	 * @return
	 * @throws IllegalArgumentException
	 */
	public ODocument get(String rid) throws IllegalArgumentException{
		Object orid=OSQLHelper.parseValue(rid, null);
		if (!(orid instanceof ORecordId)) throw new IllegalArgumentException(rid +" is not a valid rid");
		ODocument doc=get((ORecordId)orid);
		return doc;
	}
	
	public ODocument get(ORID rid) {
		ODatabaseRecordTx db =DbHelper.getConnection();
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		ODocument doc=db.load(rid);
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return doc;
	}
	
	/***
	 * Returns a _BB_NODE element by its id (not RID)
	 * @param id
	 * @return
	 */
	public ORID getRidNodeByUUID(UUID id){
		return getRidNodeByUUID(id.toString());
	}

	/***
	 * Returns a _BB_NODE element by its id (not RID)
	 * @param id
	 * @return
	 */
	public ORID getRidNodeByUUID(String id){
		ODatabaseRecordTx db =DbHelper.getConnection();
		OIndex<?> index = db.getMetadata().getIndexManager().getIndex("_BB_Node.id");
		ORID rid = (ORID) index.get(id);  
		return rid;
	}
	

	/***
	 * Execute a query against a given OrientDB Class
	 * @param oclass
	 * @param criteria
	 * @return
	 * @throws SqlInjectionException
	 */
	public List<ODocument>executeQuery(String oclass, QueryParams criteria) throws SqlInjectionException{
		OCommandRequest command = DbHelper.selectCommandBuilder(oclass, false, criteria);
		List<ODocument> result = null;
		try{
			result = DbHelper.selectCommandExecute(command, criteria.getParams());
		}catch (OCommandSQLParsingException e){
			throw new InvalidCriteriaException(e);
		}
		return result;
	}
	

	/***
	 * Executes a generic OrientDB SQL Command
	 * @param commandString
	 * @param params
	 */
	public void executeCommand(String commandString, Object[] params) {
		ODatabaseRecordTx db =  DbHelper.getConnection();
		OCommandRequest command=db.command(new OCommandSQL(commandString));
		//Logger.debug("########## is in transaction??  : " + db.getTransaction().isActive());
		command.execute(params);
	}
	
}
