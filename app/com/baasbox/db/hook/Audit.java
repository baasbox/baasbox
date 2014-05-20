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
package com.baasbox.db.hook;

import java.util.Date;

import play.Logger;

import com.baasbox.BBInternalConstants;
import com.baasbox.dao.NodeDao;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.hook.ORecordHook.RESULT;


public class Audit extends BaasBoxHook {
	
	public static Audit getIstance(){
		return new Audit();
	}
	
	protected Audit() {
		super();
	}
	
	@Override
	 public com.orientechnologies.orient.core.hook.ORecordHook.RESULT onRecordBeforeCreate(ORecord<?> iRecord){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
				if ( 
					 ( doc.field("type")!=null && !doc.field("type").equals(BBInternalConstants.FIELD_AUDIT) )
					||
					 ( doc.field("type")==null )
					){
					if(!doc.isEmbedded() && doc.getClassName()!=null && (doc.getSchemaClass().isSubClassOf(NodeDao.CLASS_NODE_NAME) || doc.getSchemaClass().getName().equals("E") || doc.getSchemaClass().isSubClassOf("E"))){
						if (Logger.isDebugEnabled()) Logger.debug("  AuditHook.onRecordBeforeCreate: creation of audit fields for document " + doc.getIdentity());
						ODocument auditDoc = new ODocument();
						Date data = new Date();
						auditDoc.field("type",BBInternalConstants.FIELD_AUDIT);
						auditDoc.field("createdBy",iRecord.getDatabase().getUser().getDocument().getIdentity());
						auditDoc.field("createdOn",data); 
						auditDoc.field("modifiedBy",iRecord.getDatabase().getUser().getDocument().getIdentity());
						auditDoc.field("modifiedOn",data);
						doc.field(BBInternalConstants.FIELD_AUDIT,auditDoc);		
						return RESULT.RECORD_CHANGED;
					}//doc.getClassName()
				}
		}//iRecord instanceof ODocument
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return RESULT.RECORD_NOT_CHANGED;
	 }//onRecordBeforeCreate

	@Override
	 public com.orientechnologies.orient.core.hook.ORecordHook.RESULT onRecordBeforeUpdate (ORecord<?> iRecord){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
				if ( 
					 ( doc.field("type")!=null && !doc.field("type").equals(BBInternalConstants.FIELD_AUDIT) )
					||
					 ( doc.field("type")==null )
					){
					if(!doc.isEmbedded() && doc.getClassName()!=null && doc.getSchemaClass().isSubClassOf(NodeDao.CLASS_NODE_NAME)){
						if (Logger.isDebugEnabled()) Logger.debug("  AuditHook.onRecordBeforeUpdate: update of audit fields for ORecord: " + iRecord.getIdentity());
						ODocument auditDoc = doc.field(BBInternalConstants.FIELD_AUDIT);
						if (auditDoc==null) auditDoc = new ODocument();
						Date data = new Date();
						auditDoc.field("modifiedBy",iRecord.getDatabase().getUser().getDocument().getIdentity());
						auditDoc.field("modifiedOn",data);
						doc.field(BBInternalConstants.FIELD_AUDIT,auditDoc);	
						return RESULT.RECORD_CHANGED;
					}
				}
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return RESULT.RECORD_NOT_CHANGED;
	 }//onRecordBeforeUpdate

	@Override
	public String getHookName() {
		return "Audit";
	}
}
