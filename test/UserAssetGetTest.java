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
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractTest;
import core.TestConfig;


public class UserAssetGetTest extends AbstractTest 
{
	private static final String FAKE_ASSET = "fakeAsset";

	@Override
	public String getRouteAddress()
	{
		return "/asset";
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
		assertJSON(json, "@rid");
		assertJSON(json, "name");
		assertJSON(json, "meta");
	}

	@Test
	public void testRouteGetFakeAsset()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress() + "/" + FAKE_ASSET+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteGetFakeAsset", Status.NOT_FOUND, null, false);
				}
			}
		);		
	}

	@Test
	public void testRouteGetAsset()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					new AdminAssetCreateTest().routeCreateAsset();

					continueOnFail(true);
					
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress() + "/" + AdminAssetCreateTest.TEST_ASSET_NAME() + "/data"+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteGetAsset", Status.OK, null, true);

					continueOnFail(false);
					
					new AdminAssetCreateTest().routeDeleteAsset();
				}
			}
		);		
	}

	@Test
	public void testRouteDownloadFakeAsset()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					new AdminAssetCreateTest().routeCreateAsset();

					continueOnFail(true);
					
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress() + "/" + FAKE_ASSET + "/download"+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteDownloadFakeAsset", Status.NOT_FOUND, null, false);

					continueOnFail(false);
					
					new AdminAssetCreateTest().routeDeleteAsset();
				}
			}
		);		
	}
	
	@Test 
	public void testServerGetFakeAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					httpRequest(getURLAddress()  + "/" + FAKE_ASSET+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE, getMethod());
					assertServer("testServerGetFakeAsset", Status.NOT_FOUND, null, false);
				}
	        }
		);
	}

	@Test
	public void testServerGetAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					new AdminAssetCreateTest().serverCreateAsset();

					continueOnFail(true);
					
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress()  + "/" + AdminAssetCreateTest.TEST_ASSET_NAME() + "/data"+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE, getMethod());
					assertServer("testServerGetAsset", Status.OK, null, true);

					continueOnFail(false);

					new AdminAssetCreateTest().serverDeleteAsset();
				}
	        }
		);
	}

	@Test
	public void testServerDownloadAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					new AdminAssetCreateTest().serverCreateFileAsset();
					
					continueOnFail(true);

					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress()  + "/" + AdminAssetCreateTest.TEST_FILE_ASSET_NAME() + "/download"+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE, getMethod());
					assertServer("testServerGetAsset", Status.OK, null, false);
					
					continueOnFail(false);

					new AdminAssetCreateTest().serverDeleteFileAsset();
				}
	        }
		);
	}

	@Test
	public void testServerDownloadFakeAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress()  + "/" + FAKE_ASSET + "/download"+"?"+TestConfig.KEY_APPCODE+"="+TestConfig.VALUE_APPCODE, getMethod());
					assertServer("testServerDownloadFakeAsset", Status.NOT_FOUND, null, false);
				}
	        }
		);
	}
	
}
