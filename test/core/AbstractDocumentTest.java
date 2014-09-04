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

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;





import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;


public abstract class AbstractDocumentTest extends AbstractRouteHeaderTest 
{
	public static final String SERVICE_ROUTE = "/document/";
	public static final String COLLECTION_NOT_EXIST = "fakeCollection";
	
	protected String getCreationDate(Object json){
		String sRet = null;
		try	{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_creation_date");
		}catch (Exception ex){
			Assert.fail("Cannot get _creation_date value: " + ex.getMessage());
		}
		return sRet;
	}
	protected String getAuthor(Object json){
		String sRet = null;

		try	{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_author");
		}	catch (Exception ex)	{
			Assert.fail("Cannot get _author value: " + ex.getMessage());
		}
		return sRet;
	}
	
	
	public static String getRouteAddress(String sCollectionName)
	{
		return SERVICE_ROUTE + sCollectionName;
	}

	public String getURLAddress(String sCollectionName)
	{
		return TestConfig.SERVER_URL + getRouteAddress(sCollectionName);
	}
	
	protected Result routeCreateDocument(String sAddress)
	{
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
		return routeAndCall(request); 
	}

	public Result routeModifyDocument(String sAddress)
	{
		// Modify created document
		FakeRequest request = new FakeRequest(PUT, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(getPayload("/documentModifyPayload.json"), PUT);
		return routeAndCall(request);
	}
	
	public Result routeGetDocument(String sAddress)
	{
		Result result = null;
		FakeRequest request = new FakeRequest(GET, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		result = routeAndCall(request);
		
		return result;
	}
	
	public void serverCreateDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httpRequest
		( 
			sAddress,
			POST,
			"/documentCreatePayload.json"
		);
	}

	public void serverModifyDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httpRequest
		( 
			sAddress,
			PUT,
			"/documentModifyPayload.json"
		);
	}
	
	public void serverGetDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest	( sAddress,	GET	);
	}
	
	@Test
	@Override
	public void testRouteNotValid() {
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// No AppCode, No Authorization
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					Result result = routeAndCall(request);
					assertRoute(result, "No AppCode No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE_NO_AUTH, true);
	
					
					// Invalid AppCode
					request = request.withHeader(TestConfig.KEY_APPCODE, "12345890");
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	
					// Invalid Authorization
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					result = routeAndCall(request);
					assertRoute(result, "Invalid Authorization", UNAUTHORIZED, null, false);
					
					// No AppCode
					request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
				}
			}
		);		
	}

	
	
	@Test
	@Override
	public void testServerNotValid() {
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					// No AppCode, No Authorization
					removeAllHeaders();
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode, No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE_NO_AUTH, true);
					
					
					// Invalid AppCode
					setHeader(TestConfig.KEY_APPCODE, "1");
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	
					// Invalid Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid Autorization", UNAUTHORIZED, null, false);
	
					// No AppCode
					removeHeader(TestConfig.KEY_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
	            }
	        }
		);
	}
}
