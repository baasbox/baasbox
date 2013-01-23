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
package com.baasbox.db;


import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

public class CustomSqlFunctions {
	
	public static void registerFunctions() {
		//registerCoalesce();
	}
	
	public static void registerCoalesce(){
		OSQLEngine.getInstance().registerFunction("coalesce", new OSQLFunctionAbstract("coalesce", 1, 1000) {



			@Override
			public String getSyntax() {
				return "Returns the first not-null parameter or null if all parameters are null. Syntax: coalesce(<field|value> [,<return_value_if_not_null>])";
			}

			@Override
			public Object execute(OIdentifiable iCurrentRecord, ODocument iCurrentResult,
					final Object[] iParameters, OCommandContext iContext) {
				int length=iParameters.length;
				for (int i=0;i<length;i++){
					if (iParameters[i]!=null) return iParameters[i];
				}
				return null;
			}
			
		});
	}
	
/*
	private static void registerExpand(){
		 // REGISTER 'EXPAND' FUNCTION WITH FIXED 2 PARAMETERS
		OSQLEngine.getInstance().registerFunction("expand", new OSQLFunctionAbstract("expand", 2, 2) {
	    	  public String getSyntax() {
	    	    return "expand(<record>, <depth>)";
	    	  }
	
	
	    	  public boolean aggregateResults() {
	    	    return false;
	    	  }
	
			@Override
			public Object execute(OIdentifiable currentObject, Object[] parameters,
					OCommandExecutor commandCaller) {
				
				String depth = (String) parameters[1];
				
				Object param = parameters[0];
				ODocument obj = null;
				if (param instanceof ORecordId){
					obj=DbHelper.getConnection().load((ORecordId)param);
				}else if (param instanceof ODocument){
					obj = (ODocument)param;
				}else if (param instanceof String){
					obj = ((ODocument)(currentObject.getRecord())).field((String)param);
				}else return null;
				if (obj==null) return null;
				ODocument ritorno = new ODocument();
				obj.copy (ritorno);
				return ritorno;
			}
		}); //OSQLEngine.getInstance().registerFunction
	}//registerExpand

*/
}
