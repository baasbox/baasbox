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


public class DeleteHook extends BaasBoxHook {


	public static DeleteHook getIstance(){
		return new DeleteHook();
	}
	
	protected DeleteHook() {
		super();
	}
	
	@Override
	 public void  onRecordAfterDelete(ORecord<?> iRecord){
		if (Logger.isDebugEnabled()) Logger.debug("Method Start");
		if (iRecord instanceof ODocument){
			ODocument doc = (ODocument)iRecord;
					if(!doc.isEmbedded() && doc.getClassName()!=null && !doc.getClassName().equals(BBInternalConstants.DELETED_CLASS_NAME) && (doc.getSchemaClass().isSubClassOf(NodeDao.CLASS_NODE_NAME) 
							//currently we dont't track deleted edges
							//|| doc.getSchemaClass().getName().equals("E") || doc.getSchemaClass().isSubClassOf("E")
							)){
						if (Logger.isDebugEnabled()) Logger.debug("NodeDatesHook.onRecordBeforeCreate: deletion date fields for document " + doc.getIdentity());
						Date now=new Date();
						ODocument storedDoc = new ODocument(BBInternalConstants.DELETED_CLASS_NAME);
						
						storedDoc.fromJSON(doc.toJSON("fetchPlan:@class:-2 @version:-2,attribSameRow,alwaysFetchEmbedded,indent:0"));
						storedDoc.field(BaasBoxPrivateFields.DELETE_DATE.toString(),now);	
						storedDoc.field(BaasBoxPrivateFields.EX_CLASS.toString(),doc.getClassName());	
						storedDoc.field(BaasBoxPrivateFields.EX_VERSION.toString(),doc.getVersion());	
						storedDoc.field(BaasBoxPrivateFields.CREATION_DATE.toString(),(Date)doc.field(BaasBoxPrivateFields.CREATION_DATE.toString()));
						storedDoc.setClassName(BBInternalConstants.DELETED_CLASS_NAME);
						storedDoc.save();
					}//doc.getClassName()
		}//iRecord instanceof ODocument
		super.onRecordAfterDelete(iRecord);
		if (Logger.isDebugEnabled()) Logger.debug("Method End");
	 }//onRecordAfterDelete

	@Override
	public String getHookName() {
		return "DeleteHook";
	}
}
