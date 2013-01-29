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

import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractUserTest;
import core.TestConfig;

public class UserUpdateTest extends AbstractUserTest 
{
	private static String JSON_MODIFIED_ATTRIBUTE = "visibleByAnonymousUsers\":{\"_allowRead\":";
	private Object json;
	
	@Override
	public String getRouteAddress()
	{
		return ROUTE_USER;
	}
	
	@Override
	public String getMethod()
	{
		return PUT;
	}
	
	@Override
	protected void assertContent(String s)
	{
		json = toJSON(s);
		assertJSON(json, "user");
	}

	@Override
	public String getDefaultPayload()
	{
		return "/adminUserUpdateNoRolePayload.json";		
	}
	
	//@Begin
	public void beginTest()
	{
		// @Todo: create user when delete user function is ready
	}
	
	@Test
	public void testRouteUpdateUser()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
					String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUser, sPwd);
					
					// Test update user
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					request = request.withJsonBody(getPayload("/adminUserUpdateNoRolePayload.json"), PUT);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteUpdateUser", Status.OK, null, true);
					assertJSONString(json, JSON_MODIFIED_ATTRIBUTE);
				}
			}
		);		
	}
	
	@Test
	public void testServerUpdateUser()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = new AdminUserFunctionalTest().serverCreateNewUser();
					String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUser, sPwd);
					
					// Test change password
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, sAuthEnc);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					( 
						getURLAddress(),
						getMethod(),
						"/adminUserUpdateNoRolePayload.json"
					);
					assertServer("testServerUpdateUser", Status.OK, null, true);
					assertJSONString(json, JSON_MODIFIED_ATTRIBUTE);
	            }
	        }
		);
	}
	
	//@After
	public void afterTest()
	{
		// @Todo: delete created user when function is ready
	}
}
