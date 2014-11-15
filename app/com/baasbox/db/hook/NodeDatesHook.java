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
import com.baasbox.service.storage.BaasBoxPrivateFields;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.hook.ORecordHook.RESULT;


public class NodeDatesHook extends BaasBoxHook {
	
	public static NodeDatesHook getIstance(){
		return new NodeDatesHook();
	}
	
	protected NodeDatesHook() {
		super();
	}
	
	@Override
	 public com.orientechnologies.orient.core.hook.ORecordHook.RESULT onRecordBeforeCreate(ORecord<?> iRecord){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
					if(!doc.isEmbedded() && doc.getClassName()!=null && !doc.getClassName().equals(BBInternalConstants.DELETED_CLASS_NAME) && (doc.getSchemaClass().isSubClassOf(NodeDao.CLASS_NODE_NAME) || doc.getSchemaClass().getName().equals("E") || doc.getSchemaClass().isSubClassOf("E"))){
						if (Logger.isDebugEnabled()) Logger.debug("NodeDatesHook.onRecordBeforeCreate: creation and update fields for document " + doc.getIdentity());
						Date now=new Date();
						doc.field(BaasBoxPrivateFields.CREATION_DATE.toString(),now);		
						doc.field(BaasBoxPrivateFields.UPDATE_DATE.toString(),now);		
						return RESULT.RECORD_CHANGED;
					}//doc.getClassName()
		}//iRecord instanceof ODocument
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return RESULT.RECORD_NOT_CHANGED;
	 }//onRecordBeforeCreate

	@Override
	 public com.orientechnologies.orient.core.hook.ORecordHook.RESULT onRecordBeforeUpdate (ORecord<?> iRecord){
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
					if(!doc.isEmbedded() && doc.getClassName()!=null && !doc.getClassName().equals(BBInternalConstants.DELETED_CLASS_NAME) && doc.getSchemaClass().isSubClassOf(NodeDao.CLASS_NODE_NAME)){
						if (Logger.isDebugEnabled()) Logger.debug("  AuditHook.onRecordBeforeUpdate: creation and update fields for ORecord: " + iRecord.getIdentity());
						Date now=new Date();
						doc.field(BaasBoxPrivateFields.UPDATE_DATE.toString(),now);		
						return RESULT.RECORD_CHANGED;
					}
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return RESULT.RECORD_NOT_CHANGED;
	 }//onRecordBeforeUpdate

	@Override
	public String getHookName() {
		return "NodeDates";
	}
}
