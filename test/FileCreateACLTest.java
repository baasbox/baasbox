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
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractAdminAssetTest;
import core.AbstractFileTest;
import core.TestConfig;


public class FileCreateACLTest extends AbstractFileTest
{

	public static final String TEST_FILE_NAME_ = "testFile";
	

	private Map<String, String> mParametersFile = new HashMap<String, String>();

	private Object json = null;
	

	
	@Override
	public String getRouteAddress()
	{
		return "/file";
	}
	
	@Override
	public String getMethod()
	{
		return POST;
	}

	@Override
	protected void assertContent(String s)
	{
		json = toJSON(s);
		assertJSON(json, "id");
		assertJSON(json, PARAM_DATA);
	}

	private String getUuid()
	{
		String sUuid = null;
		try	{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
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
					
					mParametersFile.put(PARAM_DATA, getPayload("/adminAssetCreateMeta.json").toString());
				}
			}
		);		
	}

	
	@Test
	public void testServerCreateFile()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateFile();
					
					continueOnFail(true);
				
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest(getURLAddress(), getMethod(), mParametersFile);
					assertServer("testServerCreateFile. wrong media type", Status.BAD_REQUEST, null, false);

				}
	        }
		);
	}

	
	public void serverCreateFile()
	{
		mParametersFile.put(PARAM_DATA, getPayload("/adminAssetCreateMeta.json").toString());
		mParametersFile.put("acl", "{\"read\":{\"roles\":[\"anonymous\"]}");
		

		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setMultipartFormData();
		setFile("/logo_baasbox_lp.png", "image/png");
		httpRequest(getURLAddress(), getMethod(), mParametersFile);
		assertServer("MalformedACL_error_text", Status.BAD_REQUEST, "The 'acl' field is malformed", true);
		assertServer("MalformedACL_bb_code", Status.BAD_REQUEST, "bb_code\":\"40002", true);
		
		
		mParametersFile.put("acl", "{\"read\":{\"roles\":[\"anonymous\"]}}");
		httpRequest(getURLAddress(), getMethod(), mParametersFile);
		assertServer("MalformedACL_OK", Status.OK, null, false);
		String uuid1=getUuid();
		
		//The file can be read without authentication
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		removeHeader(TestConfig.KEY_AUTH);
		httpRequest(getURLAddress()+"/"+uuid1, "GET");
		assertServer("checkACL", Status.OK, null, true);
	
		serverDeleteFile(uuid1);
	}
	
	
	public void serverDeleteFile(String id)
	{	
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest(getURLAddress() + "/" + id, DELETE);
		assertServer("testServerCreateFileAsset. Delete", Status.OK, null, false);
	}
	
}
