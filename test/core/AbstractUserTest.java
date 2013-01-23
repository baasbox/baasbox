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

//@author: Marco Tibuzzi

package core;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

public abstract class AbstractUserTest extends AbstractTest 
{
	public static final String AUTH_USER = "user1:passw1";
	public static final String AUTH_USER_CHANGED = "user1:passw2";
	public static final String AUTH_USER_ENC;
	public static final String AUTH_USER_CHANGED_ENC;
	protected static final String ROUTE_USER = "/user"; 
	
	static
	{
		AUTH_USER_ENC = TestConfig.encodeAuth(AUTH_USER);
		AUTH_USER_CHANGED_ENC = TestConfig.encodeAuth(AUTH_USER_CHANGED);
	}
	
	@Test
	public void testRouteNotValid()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// No AppCode, No Authorization
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					Result result = routeAndCall(request);
					assertRoute(result, "No AppCode No Authorization", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);

					// No Authorization
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					result = routeAndCall(request);
					assertRoute(result, "No Authorization", UNAUTHORIZED, null, false);
					
					// Invalid AppCode
					request = request.withHeader(TestConfig.KEY_APPCODE, "12345890");
					result = routeAndCall(request);
					assertRoute(result, "Invalid AppCode", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);

					// Invalid Authorization
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					result = routeAndCall(request);
					assertRoute(result, "Invalid Authorization", UNAUTHORIZED, null, false);
					
					// No AppCode
					request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "No AppCode", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);
				}
			}
		);		
	}

	@Test
	public void testServerNotValid()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					// No AppCode, No Authorization
					removeHeader(TestConfig.KEY_APPCODE);
					removeHeader(TestConfig.KEY_AUTH);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode, No Authorization", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);
					
					// No Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No Authorization", UNAUTHORIZED, null, false);
					
					// Invalid AppCode
					setHeader(TestConfig.KEY_APPCODE, "1");
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid AppCode", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);

					// Invalid Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid Autorization", UNAUTHORIZED, null, false);

					// No AppCode
					removeHeader(TestConfig.KEY_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode", BAD_REQUEST, TestConfig.MSG_INVALID_APP_CODE, true);
	            }
	        }
		);
	}
}
