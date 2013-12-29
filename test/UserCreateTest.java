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
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;

import play.api.test.Helpers;
import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractUserTest;
import core.TestConfig;

public class UserCreateTest extends AbstractUserTest
{
	public static final String USER_TEST = "user";
	
	@Override
	public String getRouteAddress()
	{
		return ROUTE_USER;
	}
	
	@Override 
	public String getMethod()
	{
		return POST;
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
					assertRoute(result, "No AppCode No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);

					// Invalid AppCode
					request = request.withHeader(TestConfig.KEY_APPCODE, "12345890");
					result = routeAndCall(request);
					assertRoute(result, "Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
				}
			}
		);		
	}

	
	@Test
	public void routeCreateUser()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = USER_TEST + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
					
					// Create user
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, getMethod());
					Result result = routeAndCall(request);
					
					assertRoute(result, "routeCreateUser", Status.CREATED, null, false);
					String body = play.test.Helpers.contentAsString(result);
					JsonNode jsonRes = Json.parse(body);
					String token = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).getTextValue();
					Assert.assertNotNull(token);
					Assert.assertFalse(SessionTokenProvider.getSessionTokenProvider().getSession(token).isEmpty());
					
				}
			}
		);		
		
	}
	
	@Test
	public void serverCreateUser()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = USER_TEST + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

					// Create user
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest
					(
						getURLAddress(),
						getMethod(),
						node
					);
					assertServer("serverCreateUser", Status.CREATED, null, false);
				}
	        }
		);
	}

	@Test
	@Override
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
					assertServer("No AppCode, No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
					
					// Invalid AppCode
					setHeader(TestConfig.KEY_APPCODE, "1");
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	            }
	        }
		);
	}
	
}
