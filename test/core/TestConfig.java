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

// @author: Marco Tibuzzi

package core;

import org.apache.commons.codec.binary.Base64;

public class TestConfig 
{
	public static final String SERVER_ADDRESS = "http://127.0.0.1";
	public static final int SERVER_PORT = 3333;
	public static final String SERVER_URL = SERVER_ADDRESS + ":" + SERVER_PORT;
	
	public static final String KEY_APPCODE = "X-BAASBOX-APPCODE";
	public static final String KEY_AUTH = "AUTHORIZATION";
	public static final String KEY_TOKEN = "X-BB-SESSION";
	
	
	public static final String VALUE_APPCODE = "1234567890";
	

	public static final String ADMIN_USERNAME = "admin";
	public static final String AUTH_ADMIN_PASS = "admin";
	public static final String AUTH_ADMIN = ADMIN_USERNAME + ":" + AUTH_ADMIN_PASS;
	public static final String AUTH_ROOT_PASSWORD = "root";
	public static final String AUTH_ROOT = "root:"+ AUTH_ROOT_PASSWORD;

	public static final String AUTH_DEFAULT = "baasbox:baasbox";
	
	public static final String TEST_COLLECTION_NAME = "documents";
	
	public static final String AUTH_ADMIN_ENC;
	public static final String AUTH_ROOT_ENC;
	public static final String AUTH_DEFAULT_ENC;
	 
	public static final String MSG_INVALID_APP_CODE = "Invalid App Code";
	public static final String MSG_NO_APP_CODE = "AppCode is empty";
	 
	public static final String MSG_USER_MODIDY_NOT_PRESENT = " is not a user";
	public static final String MSG_INVALID_COLLECTION = "is not a valid collection name";
	public static final String MSG_BAD_RID = "is not a RecordId in form of string.";
	public static final String MSG_BAD_RID_MODIFY = "is not a document";
	public static final String MSG_CHANGE_PWD = "The old password does not match with the current one";
	public static final String MSG_CHANGE_ADMIN_PWD = "The body payload doesn't contain password field";
	public static final String MSG_ASSET_ALREADY_EXISTS = "An asset with the same name already exists";
	public static final String MSG_NO_APP_CODE_NO_AUTH = "Missing Session Token, Authorization info and even the AppCode";

	static
	{
		AUTH_ADMIN_ENC = encodeAuth(AUTH_ADMIN);
		AUTH_ROOT_ENC = encodeAuth(AUTH_ROOT);
		AUTH_DEFAULT_ENC = encodeAuth(AUTH_DEFAULT);
	}
	
	public static String encodeAuth(String s)
	{
		return "Basic " + new String(Base64.encodeBase64(s.getBytes()));
	}
	
	public static String encodeAuth(String sUserName, String sPassword)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(sUserName);
		sb.append(":");
		sb.append(sPassword);
		String sRet = "Basic " + new String(Base64.encodeBase64(sb.toString().getBytes()));
		
		return sRet.replace("\n", "");
	}
}
