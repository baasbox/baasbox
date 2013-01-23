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

import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractAdminTest;
import core.TestConfig;

public class AdminUserFunctionalTest extends AbstractAdminTest
{
	private static final String USER_NOT_PRESENT = "userNotPresent";
	private static final String USER_TEST = "user1";
	
	@Override
	public String getRouteAddress()
	{
		return "/admin/user";
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}

	@Override
	protected void assertContent(String sContent)
	{
		Object obj = toJSON(sContent);
		assertJSON(obj, "user");
	}
	
	public void beforeTest()
	{
		// @Todo create test user
	}
	
	@Test
	public void testRouteUpdateNotExistentUser()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(PUT, getRouteAddress() + "/" + USER_NOT_PRESENT);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserUpdatePayload.json"), PUT);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteUpdateNotExitentUser", Status.BAD_REQUEST, USER_NOT_PRESENT + TestConfig.MSG_USER_MODIDY_NOT_PRESENT, true);
				}
			}
		);		
	}

	@Test
	public void testRouteUpdateUserOK()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(PUT, getRouteAddress() + "/" + USER_TEST);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserUpdatePayload.json"), PUT);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteUpdateUserOK", Status.OK, null, false);
				}
			}
		);		
	}
	
	@Test
	public void testServerUpdateNotExistentUser()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + USER_NOT_PRESENT,
						PUT,
						"/adminUserUpdatePayload.json"
					);
					assertServer("testServerUpdateNotExitentUser", Status.BAD_REQUEST, USER_NOT_PRESENT + TestConfig.MSG_USER_MODIDY_NOT_PRESENT, true);
				}
	        }
		);
	}
	
	@Test 
	public void testServerUpdateUserOK()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + USER_TEST,
						PUT,
						"/adminUserUpdatePayload.json"
					);
					assertServer("testServerUpdateUserOK", Status.OK, null, false);
					
					// Updates check
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(), GET);
					assertServer("testServerUpdateUserOK check", Status.OK, null, false);
					Object obj = toJSON(getResponse());
					assertJSON(obj, "user");
					JSONObject jElement = jsonFindElementByValue((JSONArray)obj, "user", "name", USER_TEST);					
					Assert.assertNotNull("Test user not found", jElement);
					// Change role check
					assertJSONString(jElement, "\"name\":\"registereduser\"");
					// Change attribute check
					assertJSONString(jElement, "\"quote\":\"I am very happy!\"");
				}
	        }
		);
	}
	
	@Test
	public void testServerUpdateUserNoRole()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + USER_TEST,
						PUT,
						"/adminUserUpdateNoRolePayload.json"
					);
					assertServer("testServerUpdateUserNoRole", Status.BAD_REQUEST, "The 'role' field is missing", true);
				}
	        }
		);
	}
	
	@Test
	public void testServerUpdateUserNotExistentRole()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + USER_TEST,
						PUT,
						"/adminUserUpdateNotExistentRole.json"
					);
					assertServer("testServerUpdateUserNoRole", Status.BAD_REQUEST, " is not a role", true);
				}
	        }
		);
	}
	
	public void afterTest()
	{
		// @Todo remove test user
	}
}
