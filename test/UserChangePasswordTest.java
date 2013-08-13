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
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import play.Logger;
import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractUserTest;
import core.TestConfig;


public class UserChangePasswordTest extends AbstractUserTest
{
	@Override
	public String getRouteAddress()
	{
		return ROUTE_USER + "/password";
	}
	
	@Override 
	public String getMethod()
	{
		return PUT;
	}
	
	@Override
	protected void assertContent(String s)
	{
	}
	
	@Override
	public String getDefaultPayload()
	{
		return "/userChangePasswordPayload.json";
	}

	@Test
	public void testRouteChangePassword()
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
					
					// Test change password
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					request = request.withJsonBody(getPayload("/userChangePasswordPayload.json"), getMethod());
					Result result = routeAndCall(request);
					Logger.debug("testRouteChangePassword request: " + request.getWrappedRequest().headers());
					Logger.debug("testRouteChangePassword result: " + contentAsString(result));
					assertRoute(result, "testRouteChangePassword", Status.OK, null, false);

					String sPwdChanged = getPayloadFieldValue("/userChangePasswordPayload.json", "new");
					String sAuthChanged = TestConfig.encodeAuth(sFakeUser, sPwdChanged);
					
					continueOnFail(true);
					
					// Test change password non valid
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthChanged);
					request = request.withJsonBody(getPayload("/userChangePasswordInvalid.json"), getMethod());
					result = routeAndCall(request);
					assertRoute(result, "testRouteChangePassword not valid", Status.BAD_REQUEST, TestConfig.MSG_CHANGE_PWD, true);

					continueOnFail(false);
					
					// Restore old password
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthChanged);
					request = request.withJsonBody(getPayload("/userRestorePasswordPayload.json"), getMethod());
					result = routeAndCall(request);
					assertRoute(result, "testRouteChangePassword restore old password", Status.OK, null, false);
				}
			}
		);		
	}
	
	@Test
	public void testServerChangePassword()
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
						"/userChangePasswordPayload.json"
					);
					assertServer("testServerChangePassword", Status.OK, null, false);

					String sPwdChanged = getPayloadFieldValue("/userChangePasswordPayload.json", "new");
					String sAuthChanged = TestConfig.encodeAuth(sFakeUser, sPwdChanged);
					
					continueOnFail(true);

					// Test change password non valid
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, sAuthChanged);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					( 
						getURLAddress(),
						getMethod(),
						"/userChangePasswordInvalid.json"
					);
					assertServer("testServerChangePassword not valid", Status.BAD_REQUEST, TestConfig.MSG_CHANGE_PWD, true);

					continueOnFail(false);

					// Restore old password
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, sAuthChanged);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					( 
						getURLAddress(),
						getMethod(),
						"/userRestorePasswordPayload.json"
					);
					assertServer("testServerChangePassword restore old password", Status.OK, null, false);
	            }
	        }
		);
	}
}
