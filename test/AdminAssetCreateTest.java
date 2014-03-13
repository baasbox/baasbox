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

import static play.test.Helpers.DELETE;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractAdminAssetTest;
import core.TestConfig;


public class AdminAssetCreateTest extends AbstractAdminAssetTest
{
	public static final String TEST_ASSET_NAME_ = "testSimpleAsset";
	public static final String TEST_FILE_ASSET_NAME_ = "testFileAsset";
	
	private Map<String, String> mParametersSimple = new HashMap<String, String>();
	private Map<String, String> mParametersFile = new HashMap<String, String>();
	
	private static  String assetName;
	public static String TEST_ASSET_NAME(){
		if (assetName == null ) assetName = TEST_ASSET_NAME_ + UUID.randomUUID();
		return assetName;
	}
	
	private static String assetFileName;
	public static String TEST_FILE_ASSET_NAME(){
		if (assetFileName == null ) assetFileName = TEST_FILE_ASSET_NAME_ + UUID.randomUUID();
		return assetFileName;
	}
	
	@Override
	public String getRouteAddress()
	{
		return "/admin/asset";
	}
	
	@Override
	public String getMethod()
	{
		return POST;
	}

	@Override
	protected void assertContent(String s)
	{
		Object json = toJSON(s);
		assertJSON(json, "@rid");
		assertJSON(json, "name");
	}

	@Before
	public void beforeTest()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					mParametersSimple.put(PARAM_NAME, TEST_ASSET_NAME());
					mParametersSimple.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());
					
					mParametersFile.put(PARAM_NAME, TEST_FILE_ASSET_NAME());
					mParametersFile.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());
				}
			}
		);		
	}
	
	
	@Test
	public void testRouteCreateSimpleAsset()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					routeCreateAsset();

					continueOnFail(true);

					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					request = request.withFormUrlEncodedBody(mParametersSimple);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteCreateSimpleAsset. Already exists", Status.BAD_REQUEST, TestConfig.MSG_ASSET_ALREADY_EXISTS, true);

					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					request = request.withFormUrlEncodedBody(mParametersSimple);
					result = routeAndCall(request);
					assertRoute(result, "testRouteCreateSimpleAsset. Wrong media type", Status.BAD_REQUEST, null, false);
					
					continueOnFail(false);

					routeDeleteAsset();
				}
			}
		);		
	}
	
	
	
	@Test
	public void testServerCreateSimpleAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateAsset();
					
					continueOnFail(true);

					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(), getMethod(), mParametersSimple);
					assertServer("testServerCreateSimpleAsset. Already extists", Status.BAD_REQUEST, TestConfig.MSG_ASSET_ALREADY_EXISTS, true);

					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest(getURLAddress(), getMethod(), mParametersSimple);
					assertServer("testServerCreateSimpleAsset. wrong media type", Status.BAD_REQUEST, null, false);
					
					continueOnFail(false);

					serverDeleteAsset();
				}
	        }
		);
	}
	
	
	@Test
	public void testServerCreateFileAsset()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateFileAsset();

					continueOnFail(true);
				
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest(getURLAddress(), getMethod(), mParametersFile);
					assertServer("testServerCreateFileAsset. wrong media type", Status.BAD_REQUEST, null, false);
					
					continueOnFail(false);

					serverDeleteFileAsset();
				}
	        }
		);
	}
	
	public void routeCreateAsset()
	{
		mParametersSimple.put(PARAM_NAME, TEST_ASSET_NAME());
		mParametersSimple.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());

		FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		request = request.withFormUrlEncodedBody(mParametersSimple);
		Result result = routeAndCall(request);
		assertRoute(result, "testRouteCreateSimpleAsset", Status.CREATED, null, true);
	}

	public void routeDeleteAsset()
	{
		mParametersSimple.put(PARAM_NAME, TEST_ASSET_NAME());
		mParametersSimple.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());

		FakeRequest request = new FakeRequest(DELETE, getRouteAddress() + "/" + mParametersSimple.get(PARAM_NAME));
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		request = request.withFormUrlEncodedBody(mParametersSimple);
		Result result = routeAndCall(request);
		assertRoute(result, "testRouteCreateSimpleAsset. Delete", Status.OK, null, false);
		
	}
	
	public void serverCreateAsset()
	{
		mParametersSimple.put(PARAM_NAME, TEST_ASSET_NAME());
		mParametersSimple.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());

		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest(getURLAddress(), getMethod(), mParametersSimple);
		assertServer("testServerCreateSimpleAsset", Status.CREATED, null, true);
	}
	
	public void serverDeleteAsset()
	{
		mParametersSimple.put(PARAM_NAME, TEST_ASSET_NAME());
		mParametersSimple.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());

		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest(getURLAddress() + "/" + mParametersSimple.get(PARAM_NAME), DELETE);
		assertServer("testServerCreateSimpleAsset. Delete", Status.OK, null, false);
	}
	
	public void serverCreateFileAsset()
	{
		mParametersFile.put(PARAM_NAME, TEST_FILE_ASSET_NAME());
		mParametersFile.put(PARAM_META, getPayload("/adminAssetCreateMeta.json").toString());

		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setMultipartFormData();
		setAssetFile("/logo_baasbox_lp.png", "image/png");
		httpRequest(getURLAddress(), getMethod(), mParametersFile);
		assertServer("testServerCreateFileAsset", Status.CREATED, null, true);
	}
	
	public void serverDeleteFileAsset()
	{
		mParametersFile.put(PARAM_NAME, TEST_FILE_ASSET_NAME());
		
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest(getURLAddress() + "/" + mParametersFile.get(PARAM_NAME), DELETE);
		assertServer("testServerCreateFileAsset. Delete", Status.OK, null, false);
		
	}
}
