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
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
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
	private static final String USER_TEST = "user";
	private static final String USER_NOT_PRESENT = "userNotPresent";
	
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

	protected void assertCheckUserUpdate(String sContent, String sUserName)
	{
		Object obj = toJSON(sContent);
		assertJSON(obj, "user");
		// Change role check
		assertJSONString(obj, "\"name\":\"registered\"");
		// Change attribute check
		assertJSONString(obj, "\"quote\":\"I am very happy!\"");
	}
	
	@Test
	public void testRouteUpdateNotExistentUser()
	{
		running
		(
			getFakeApplication(), 
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
	public void testRouteCreateAndUpdateUser()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = routeCreateNewUser();
					
					// Update user
					FakeRequest request = new FakeRequest(PUT, getRouteAddress() + "/" + sFakeUser);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserUpdatePayload.json"), PUT);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteCreateAndUpdateUser: Update user.", Status.OK, null, false);
					
					String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					
					// Updates check
					request = new FakeRequest(GET, "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, sPwd));
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteCreateAndUpdateUser: Check updated user.", Status.OK, null, false);
					assertRoute(result, "testRouteCreateAndUpdateUser check username", Status.OK, "name\":\""+sFakeUser+"\"", true);
					assertRoute(result, "testRouteCreateAndUpdateUser check role", Status.OK, "roles\":[{\"name\":\"registered\",\"isrole\":true}", true);
					
					assertCheckUserUpdate(contentAsString(result), sFakeUser);
				}
			}
		);		
	}

	@Test
	public void testServerUpdateNotExistentUser()
	{
		running
		(
			getTestServer(), 
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
	public void testServerCreateAndUpdateUser()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = serverCreateNewUser();
					
					// Update user
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + sFakeUser,
						PUT,
						"/adminUserUpdatePayload.json"
					);
					assertServer("testServerCreateAndUpdateUser: Update user.", Status.OK, null, false);
					
					String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					
					// Updates check
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, sPwd));
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(TestConfig.SERVER_URL + "/user", GET);
					assertServer("testServerCreateAndUpdateUser: Check updated user.", Status.OK, null, false);
					
					assertCheckUserUpdate(getResponse(), sFakeUser);
				}
	        }
		);
	}
	
	@Test
	public void testServerUpdateUserNoRole()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = serverCreateNewUser();
					
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + sFakeUser,
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
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = serverCreateNewUser();
					
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress() + "/" + sFakeUser,
						PUT,
						"/adminUserUpdateNotExistentRole.json"
					);
					assertServer("testServerUpdateUserNoRole", Status.BAD_REQUEST, " is not a role", true);
				}
	        }
		);
	}
	
	public String routeCreateNewUser()
	{
		String sFakeUser = USER_TEST + UUID.randomUUID();
		// Prepare test user
		JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
		
		// Create user
		FakeRequest request = new FakeRequest(POST, getRouteAddress());
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(node, POST);
		Result result = routeAndCall(request);
		assertRoute(result, "Create user.", Status.CREATED, null, false);

		return sFakeUser;
	}
	
	public String serverCreateNewUser()
	{
		String sFakeUser = USER_TEST + UUID.randomUUID();
		// Prepare test user
		JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

		// Create user
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httpRequest
		(
			getURLAddress(),
			POST,
			node
		);
		assertServer("Create user.", Status.CREATED, null, false);

		return sFakeUser;
	}
}
