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

package com.baasbox.dao;

import java.util.List;

import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.IndexNotFoundException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

public abstract class IndexDao{
	public final String INDEX_NAME;
	public static final String MODEL_NAME = "_BB_Index";
	protected ODatabaseRecordTx db;
	
	protected IndexDao(String indexName) throws IndexNotFoundException {
		this.INDEX_NAME=indexName.toUpperCase();
		this.db=DbHelper.getConnection();
		//this.index = db.getMetadata().getIndexManager().getIndex(indexName);
		//if (index==null) throw new IndexNotFoundException("The index " + indexName + " does not exists");
		//if (!index.getType().equals(OClass.INDEX_TYPE.DICTIONARY.toString())) throw new IndexNotFoundException("The index " + indexName + " is not a dictionary");
	}
	
	public IndexDao put (String key,Object value){
		String indexKey = this.INDEX_NAME+":"+key;
		ODocument newValue = getODocument(key); 
		if(newValue==null){
			newValue = new ODocument(MODEL_NAME);
			newValue.field("key",indexKey);
		}
		
		newValue.field("value",value);
		newValue.save();
		//index.put(key, newValue);
		return this;
	}
	
	private ODocument getODocument(String key){
		String indexKey = this.INDEX_NAME+":"+key;
		QueryParams qp = QueryParams.getInstance();
		qp.where("key = ?").params(new String[]{indexKey});
		try{
			List<ODocument> docs = GenericDao.getInstance().executeQuery(MODEL_NAME, qp);
			if(docs==null || docs.isEmpty()){
				return null;
			}else{
				return docs.get(0);
			}
		}catch(SqlInjectionException sie){
			throw new RuntimeException(sie);
		}
	}
	
	public Object get (String key){
		ODocument valueOnDb=getODocument(key);
		if (valueOnDb==null) return null;
		return valueOnDb.field("value");
	}
}
