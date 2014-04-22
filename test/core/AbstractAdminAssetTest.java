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
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.apache.http.protocol.HTTP;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

public abstract class AbstractAdminAssetTest extends AbstractRouteHeaderTest
{
	public static final String PARAM_NAME = "name";
	public static final String PARAM_META = "meta";
	
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
					removeHeader(HTTP.CONTENT_TYPE);
					httpRequest(getURLAddress(), getMethod());
					assertServer("testServerDefaultUser", FORBIDDEN, null, false);
	            }
	        }
		);
	}
}
