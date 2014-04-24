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

package core;

import static play.mvc.Http.Status.FORBIDDEN;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.Before;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

public abstract class AbstractRootTest extends AbstractRouteHeaderTest
{
	
	@Test
	public void testMalformedBasicAuthHeader()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// Default user credentials
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, "BASIC");
					Result result = routeAndCall(request);
					assertRoute(result, "testMalformedBasicAuthHeader", UNAUTHORIZED, null, false);
				}
			}
		);
	}

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
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteDefaultUser", FORBIDDEN, null, false);
				}
			}
		);		
	}
	
	@Test 
	public void testRouteAdminUser()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// Default user credentials
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteAdminUser", UNAUTHORIZED, null, false);
				}
			}
		);		
	}
	

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
					resetHeaders();
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					httpRequest(getURLAddress(), getMethod());
					assertServer("testServerDefaultUser", FORBIDDEN, null, false);
	            }
	        }
		);
	}
	

}
