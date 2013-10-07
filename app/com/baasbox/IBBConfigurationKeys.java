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
package com.baasbox;

public interface IBBConfigurationKeys {
	public static final String ADMIN_USERNAME = "orient.baasbox.admin_user";
	public static final String ADMIN_PASSWORD = "orient.baasbox.admin_password";
	public static final String ANONYMOUS_USERNAME = "orient.baasbox.user";
	public static final String ANONYMOUS_PASSWORD = "orient.baasbox.password";
	public static final String DB_PATH = "orient.baasbox.path";
	public static final String DB_BACKUP_PATH = "orient.baasbox.backup.path";
	@Deprecated
	public static final String REALM = "authorization.basic.realm";
	public static final String APP_CODE = "application.code";
	public static final String API_VERSION = "api.version";
	public static final String QUERY_RECORD_PER_PAGE = "query.record_per_page";
	public static final String QUERY_RECORD_DEPTH = "query.record_depth";
	public static final String WRAP_RESPONSE="baasbox.wrapresponse";
	
	public static final String PUSH_CERTIFICATES_FOLDER = "push.baasbox.certificates.folder";
	public static final String MVCC_MAX_RETRIES = "orient.baasbox.MVCC.maxRetries";
	
}
