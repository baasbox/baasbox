package com.baasbox.dao;

import play.Logger;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.IndexNotFoundException;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexDictionary;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public abstract class IndexDao {
	public final String INDEX_NAME;
	protected OGraphDatabase db;
	protected OIndex index;

	protected IndexDao(String indexName) throws IndexNotFoundException {
		this.INDEX_NAME=indexName;
		this.db=DbHelper.getConnection();
		this.index = db.getMetadata().getIndexManager().getIndex(indexName);
		if (index==null) throw new IndexNotFoundException("The index " + indexName + " does not exists");
		if (!index.getType().equals(OClass.INDEX_TYPE.DICTIONARY.toString())) throw new IndexNotFoundException("The index " + indexName + " is not a dictionary");
	}
	
	public IndexDao put (String key,Object value){
		ODocument newValue = new ODocument();
		newValue.field("value",value);
		final OIdentifiable oldValue = (OIdentifiable) index.get(key);
		if (oldValue != null) // DELETES THE PREVIOUS INDEXED RECORD
			oldValue.getRecord().delete();
		index.put(key, newValue);
		return this;
	}
	
	
	public Object get (String key){
		ODocument value=null;
		Object valueOnDb=index.get(key);
		if (valueOnDb==null) return null;
		if (valueOnDb instanceof ORID)
			value = db.load((ORID)valueOnDb);
		else value=(ODocument)valueOnDb;
		return value.field("value");
	}
}
