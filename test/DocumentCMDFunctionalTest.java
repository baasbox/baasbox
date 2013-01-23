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

import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.status;
import static play.test.Helpers.testServer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentCMDFunctionalTest extends AbstractDocumentTest
{
	public static final String SERVICE_ROUTE = "/document/";
	public static final String COLLECTION_NOT_EXIST = "fakeCollection";
	private static final String TEST_MODIFY_JSON = "\"shape\":\"round\"";
	
	private Object json = null;
	
	@Override
	public String getRouteAddress()
	{
		return SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}
	
	@Override
	protected void assertContent(String s)
	{
		json = toJSON(s);
		assertJSON(json, "@rid");
		assertJSON(json, "color");
		assertJSON(json, "shape");
	}

	@Test
	public void testRouteCollectionNotExists()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					Result result = routeCreateDocument(SERVICE_ROUTE + COLLECTION_NOT_EXIST);
					assertRoute(result, "testRouteCollectionNotExists", Status.NOT_FOUND, null, false);
				}
			}
		);		
	}
	
	@Test
	public void testRouteCMDDocument()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					Result result = routeCreateDocument(getRouteAddress());
					assertRoute(result, "testRouteCMDDocument CREATE", Status.OK, null, true);
					String sRid = getRid();
				
			 		continueOnFail(true);
					
					try
					{
						// Test successful modify
						Result sucessModify = routeModifyDocument(getRouteAddress() + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(sucessModify, "testRouteCMDDocument MODIFY RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with non existent collection
						Result badModify = routeModifyDocument(SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(badModify, "testRouteCMDDocument no collection MODIFY RID <" + sRid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with non existent RID
						Result badModify = routeModifyDocument(getRouteAddress() + "/" + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertRoute(badModify, "testRouteCMDDocument not existent RID. MODIFY RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Test modify with inconsistent RID
						result = routeModifyDocument(getRouteAddress() + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testServerCMDDocument bad RID. MODIFY RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection
						result = routeGetDocument(getRouteAddress() + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a collection not existent
						result = routeGetDocument(SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument not existent collection get document RID <" + sRid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						result = routeGetDocument(getRouteAddress() + "/" + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						result = routeGetDocument(getRouteAddress() + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document bad RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document 2 RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection not existent RID
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document 2 not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Mode 2 Retrieve document in a collection not existent RID
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document 2 bad RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					continueOnFail(false);
					
					// Delete document
					result = routeDeleteDocument(sRid);
					assertRoute(result, "testRouteCMDDocument DELETE RID <" + sRid + ">", Status.NO_CONTENT, null, false);
				}
			}
		);		
	}

	@Test
	public void testServerCollectionNotExists()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + COLLECTION_NOT_EXIST);
					assertServer("testServerCollectionNotExists", Status.NOT_FOUND, null, false);
				}
	        }
		);
	}

	@Test
	public void testServerCMDDocument()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateDocument(getURLAddress());
					assertServer("testServerCMDDocument CREATE", Status.OK, null, true);
					String sRid = getRid();

					continueOnFail(true);
					
					try
					{
						// Test successful modify
						serverModifyDocument(getURLAddress() + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument MODIFY RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with non existent collection
						serverModifyDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument no collection MODIFY RID <" + sRid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with non existent RID
						serverModifyDocument(getURLAddress() + "/" + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument not existent RID. MODIFY", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with inconsistent RID
						serverModifyDocument(getURLAddress() + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument bad RID. MODIFY RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a collection
						serverGetDocument(getURLAddress() + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument get document RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a collection not existent
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument not existent collection get document RID <" + sRid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						serverGetDocument(getURLAddress() + "/" + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						serverGetDocument(getURLAddress() + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document incosistent RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument get document 2 RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection not existent RID
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode("#1:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document 2 not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Mode 2 Retrieve document in a collection bad RID
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document 2 bad RID", Status.NOT_FOUND, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					continueOnFail(false);

					// Delete created document
					serverDeleteDocument(sRid);
					assertServer("testServerCMDDocument DELETE RID <" + sRid + ">", Status.NO_CONTENT, null, false);
				}
	        }
		);
	}
	
	// @Todo activate test when remove collection is ready
	public void beforeTest()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(POST, new AdminCollectionFunctionalTest().getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					Result result = routeAndCall(request);
					Assert.assertEquals("beforeTest. Status", Status.CREATED, status(result));
				}
			}
		);
	}

	// @Todo activate test when remove collection is ready
	public void afterTest()
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// remove test collection
				}
			}
		);		
	}

	private String getRid()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getString("@rid");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get RID value");
		}
		
		return sRet;
	}
	
	public static void main(String[] asArgs)
	{
		try
		{
			System.out.println(java.net.URLEncoder.encode("#1", "ISO-8859-1"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
