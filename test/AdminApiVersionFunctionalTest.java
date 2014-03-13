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
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import static play.test.Helpers.testServer;
import static play.mvc.Http.Status.OK;

import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractAdminTest;
import core.TestConfig;

public class AdminApiVersionFunctionalTest extends AbstractAdminTest
{
	@Override
	public String getRouteAddress()
	{
		return "/admin/apiVersion";
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
		assertJSON(obj, "api_version");
	}
	
	@Override
	@Test 
	public void testRouteDefaultUser()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// Default user credentials
					FakeRequest request = new FakeRequest(GET, getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteDefaultUser", OK, null, true);
				}
			}
		);		
	}

	@Override
	@Test 
	public void testServerDefaultUser()
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
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					httpRequest(getURLAddress(), getMethod());
					assertServer("testServerDefaultUser", OK, null, true);
	            }
	        }
		);
	}

	@Override
	protected void assertRoute(Result result, String sTestName, int nExptedStatus, String sExpctedContent, boolean fCheckContent)
	{
		Assert.assertNotNull(sTestName + ". Cannot route to <" + getRouteAddress() + ">", result);
		Assert.assertEquals(sTestName + ". Status", OK, status(result));
		String sContent = contentAsString(result);
		assertContent(sContent);
	}
	
	@Override
	protected void assertServer(String sTestName, int nExptedStatus, String sExpctedContent, boolean fCheckContent)
	{
		Assert.assertEquals(sTestName + ". Status", OK, getStatusCode());
		assertContent(getResponse());
	}
}
