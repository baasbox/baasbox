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

import static org.junit.Assert.fail;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.GET;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;

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
	
	@Override
	public void testServerNotValid()
	{
	}
	
}
