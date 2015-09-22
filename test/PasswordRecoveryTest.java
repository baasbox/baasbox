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

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.test.Helpers.GET;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.PasswordRecovery;
import com.baasbox.exception.InvalidAppCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import core.AbstractUserTest;
import core.TestConfig;

public class PasswordRecoveryTest extends AbstractUserTest
{
	public static final String USER_TEST = "user";
	
	String ROUTE_USER="/user";
	
	@Override
	public String getRouteAddress()
	{
		return ROUTE_USER;
	}
	
	@Override 
	public String getMethod()
	{
		return GET;
	}
	
	@Override
	protected void assertContent(String s)
	{
	}
	
	@Override
	public String getDefaultPayload()
	{
		return "/adminUserCreatePayload.json";
	}
	
	
	@Test
	public void testEmailAddress()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = USER_TEST + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

					// Create user
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "testEmailAddress.createuser", Status.CREATED, null, false);
					
					// try to recover the password
					request = new FakeRequest("GET", "/user/"+sFakeUser+"/password/reset");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					result = routeAndCall(request);
					assertRoute(result, "testEmailAddress.resetpwd", BAD_REQUEST, "Cannot reset password, the \\\"email\\\" attribute is not defined into the user's private profile", true);	
				}
			}
		);	
	}
		
		@Test
		public void testToken()
		{
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sFakeUser = USER_TEST + UUID.randomUUID();
						// Prepare test user
						String newPassword="password";
						JsonNode node = updatePayloadFieldValue("/adminUserCreatePayloadForPasswordRecovery.json", "username", sFakeUser);
						String oldPassword=node.get("password").toString(); 

						// Create user
						FakeRequest request = new FakeRequest("POST", "/user");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withJsonBody(node, "POST");
						Result result = routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.createuser", Status.CREATED, null, false);
						
						// try to recover the password [step1]
						request = new FakeRequest("GET", "/user/"+sFakeUser+"/password/reset");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						result = routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.resetpwdStep1", BAD_REQUEST, "Cannot send mail to reset the password:  Could not reach the mail server. Please contact the server administrator", true);	
					
						// try to recover the password [step2]
						ODatabaseRecordTx db = null;
						
						try {
							 db = com.baasbox.db.DbHelper.getOrOpenConnection(TestConfig.VALUE_APPCODE, TestConfig.ADMIN_USERNAME, TestConfig.AUTH_ADMIN_PASS);
						} catch (InvalidAppCodeException e) {
							// TODO Auto-generated catch block
							Assert.fail("problem with reset password");
						}
						
						String timeBeforeExpiration = String.valueOf(PasswordRecovery.EMAIL_EXPIRATION_TIME.getValueAsInteger()*60*1000);
						
						List<ODocument> sqlresult = (List<ODocument>) com.baasbox.db.DbHelper.genericSQLStatementExecute("select base64_code_step1 from _BB_ResetPwd where user.user.name='"+sFakeUser+"'",null);

						String token1=sqlresult.get(0).field("base64_code_step1");
						String token1JSON=token1.concat(".json");
						
						//step 2
						request=new FakeRequest("GET", "/user/password/reset/"+token1JSON);
						result = routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.resetpwdStep2 - 1", Status.OK, null,false);
						
						sqlresult = (List<ODocument>) com.baasbox.db.DbHelper.genericSQLStatementExecute("select base64_code_step2 from _BB_ResetPwd where user.user.name='"+sFakeUser+"'",null);

						String token2=sqlresult.get(0).field("base64_code_step2");
						String token2JSON=token2.concat(".json");
						
						assertRoute(result, "testTokenPasswordRecovery.resetpwdStep2 - 2", Status.OK, "{\"user_name\":\""+sFakeUser+"\",\"link\":\"/user/password/reset/"+token2JSON+"\",\"token\":\""+token2+"\",\"application_name\":\""+com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString()+"\"}", true);	

						//step 3
						
						request=new FakeRequest("POST", "/user/password/reset/"+token2JSON);
						Map<String,String> urlEncoded = new HashMap();
						urlEncoded.put("password",newPassword);
						urlEncoded.put("repeat-password",newPassword);
						request.withFormUrlEncodedBody(urlEncoded);
						
						result = routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.resetpwdStep3", Status.OK, "{\"user_name\":\""+sFakeUser+"\",\"message\":\"Password changed\",\"application_name\":\""+com.baasbox.configuration.Application.APPLICATION_NAME.getValueAsString()+"\"}", true);	

						//try to authenticate with older password
						
						String sAuthEnc = TestConfig.encodeAuth(sFakeUser, oldPassword);

						request = new FakeRequest("GET","/me");
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						result=routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.loginWithOlderPassword", Status.UNAUTHORIZED, "{\"result\":\"error\",\"message\":\"User "+sFakeUser+" is not authorized to access\",\"resource\":\"/me\",\"method\":\"GET\",\"request_header\":{\"AUTHORIZATION\":[\""+sAuthEnc+"\"],\"X-BAASBOX-APPCODE\":[\""+TestConfig.VALUE_APPCODE+"\"]},\"API_version\":\""+BBConfiguration.getApiVersion()+"\",\"db_schema_version\":\"" + BBConfiguration.getDBVersion() + "\",\"http_code\":"+Status.UNAUTHORIZED+"}", true);	

						//try to authenticate with newer password
						sAuthEnc=TestConfig.encodeAuth(sFakeUser,newPassword);
						request = new FakeRequest("GET","/me");
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						result=routeAndCall(request);
						assertRoute(result, "testTokenPasswordRecovery.loginWithNewerPassword",Status.OK,"", false);	
						
					}
				}
			);		
		
	}
	
	@Override
	public void testServerNotValid()
	{
	}
	
}
