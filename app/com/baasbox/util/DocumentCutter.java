package com.baasbox.util;

import java.util.HashSet;

import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class DocumentCutter {
	private ODocument theDoc = null;
	private HashSet<ODocument> docsVisited = new HashSet<ODocument>();
	
	public DocumentCutter(ODocument doc){
		theDoc=doc;
	}
	
	private ODocument getCuttedDocInternal(ODocument doc,boolean preserveAcl){
		if (!docsVisited.contains(doc)){
			docsVisited.add(doc);
			for (BaasBoxPrivateFields r : BaasBoxPrivateFields.values()){
				boolean shouldBePreserved = (preserveAcl && r.isAclField()) || r.isVisibleByTheClient();
				if (!shouldBePreserved) doc.removeField(r.toString());
			}
			if (doc.getClassName()!=null && doc.getClassName().equalsIgnoreCase("ouser")) doc.removeField("password");
			for(String s:doc.fieldNames()){
				 if(doc.field(s) !=null && doc.fieldType(s)!=null && doc.fieldType(s).equals(OType.STRING) && ((String)doc.field(s)).contains("{SHA-256}")) doc.removeField(s);
	             if(doc.field(s) !=null && doc.field(s) instanceof ODocument){
	                     doc.field(s, getCuttedDocInternal((ODocument)doc.field(s),preserveAcl));
	             }
			}
		}
		return doc;
	}
	
	public ODocument getCuttedDoc(){
		return getCuttedDocInternal(theDoc,false);
	}
	
	public ODocument getCuttedDoc(boolean preserveAcl){
		if (!preserveAcl) return getCuttedDoc();
		else return getCuttedDocInternal(theDoc,preserveAcl);
	}
}
