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
import static play.test.Helpers.POST;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import static play.test.Helpers.testServer;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractFileTest;
import core.TestConfig;


public class FileGetTest extends AbstractFileTest{
	
	private Object json = null;
	
	private String sTestName="FileGetTest";
	
	@Override
	public String getRouteAddress()
	{
		return "/file";
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}


	@Override
	protected void assertContent(String s)	{
		json = toJSON(s);
	}
	
	private String getUuid(){
		String sUuid = null;
		try	{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
	}
	
	@Test
	public void testRouteGet()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					Map<String, String> mParametersFile = new HashMap<String, String>();
					mParametersFile.put(PARAM_DATA, getPayload("/adminAssetCreateMeta.json").toString());
					//create a file
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setFile("/logo_baasbox_lp.png", "image/png");
					httpRequest(getURLAddress(), POST, mParametersFile);
					assertServer(sTestName + " create", Status.CREATED, null, true);
					String uuid1=getUuid();
					
					//get it
					FakeRequest request = new FakeRequest(GET, getRouteAddress() + "/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					String contentType=play.test.Helpers.header("Content-Type", result);
					Assert.assertEquals(sTestName + " download 1", "image/png", contentType);
					
					//download it
					request = new FakeRequest(GET, getRouteAddress() + "/download/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					String contentDisposition=play.test.Helpers.header("Content-Disposition", result);
					Assert.assertEquals(sTestName + " download 2", "attachment; filename=\"logo_baasbox_lp.png\"", contentDisposition);
					
					//get its attached data
					request = new FakeRequest(GET, getRouteAddress() + "/attachedData/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result1 = routeAndCall(request);
					assertRoute(result1, sTestName + " attachedData", Status.OK, "\"name\":\"Margherita\"", true);
				}
			}
		);		
	}

	@Test
	public void testServerGet()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(), getMethod());
					assertServer("testServerGet", Status.OK, null, false);
				}
	        }
		);
	}
}
