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
package com.baasbox.util;

public interface IQueryParametersKeys {
	public static final String FIELDS="fields";
	public static final String WHERE="where";
	public static final String PAGE="page";
	public static final String DEPTH="depth";
	public static final String COUNT="count";
	public static final String RECORDS_PER_PAGE="recordsPerPage";
	public static final String ORDER_BY="orderBy";
	public static final String GROUP_BY="groupBy";
	public static final String PARAMS="params";
	public static final String SKIP="skip";
	
	
	public static final String QUERY_PARAMETERS="qryp";
	
	@Deprecated
	public static final String RECORD_PER_PAGE="recordPerPage";
	
}
