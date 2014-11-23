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

import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractUserTest;
import core.TestConfig;

public class UserGetCurrentTest extends AbstractUserTest
{
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
		Object json = toJSON(s);
		assertJSON(json, "user");
	}

	@Test
	public void testRouteGetCurrentUser()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "RouteOK Admin user", Status.OK, null, true);
					
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					result = routeAndCall(request);
					assertRoute(result, "RouteOK BaasBox user", Status.FORBIDDEN, null, false);
					
					//registered user
					String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
					String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUser, sPwd);
					
					// Test update user
					request = new FakeRequest("GET", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetCurrentUser - registered - username", Status.OK, "name\":\""+sFakeUser+"\"", true);
					assertRoute(result, "testRouteGetCurrentUser - registered - role", Status.OK, "roles\":[{\"name\":\"registered\",\"isrole\":true}", true);
					
				}
			}
		);		
	}

	@Test
	public void testServerGetCurrentUser()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					// Admin user
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod());
					assertServer("ServerOK Admin user", OK, null, true);
					
					// BassBoxUser
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					httpRequest(getURLAddress(), getMethod());
					assertServer("ServerOK BaasBox user", Status.FORBIDDEN, null, false);
	            }
	        }
		);
	}
}
