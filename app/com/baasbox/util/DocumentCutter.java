package com.baasbox.util;

import java.util.HashSet;

import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class DocumentCutter {
	private ODocument theDoc = null;
	private HashSet<ODocument> docsVisited = new HashSet<ODocument>();
	
	public DocumentCutter(ODocument doc){
		theDoc=doc;
	}
	
	private ODocument getCuttedDocInternal(ODocument doc){
		if (!docsVisited.contains(doc)){
			docsVisited.add(doc);
			for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
				if (!r.isVisibleByTheClient()) doc.removeField(r.toString());
			}
			for(String s:doc.fieldNames()){
	             if(doc.field(s) !=null && doc.field(s) instanceof ODocument){
	                     doc.field(s, getCuttedDocInternal((ODocument)doc.field(s)));
	             }
			}
		}
		return doc;
	}
	
	public ODocument getCuttedDoc(){
		return getCuttedDocInternal(theDoc);
	}
}
