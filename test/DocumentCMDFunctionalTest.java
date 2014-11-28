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
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentCMDFunctionalTest extends AbstractDocumentTest
{
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
		assertJSON(json, "id");
		assertJSON(json, "color");
		assertJSON(json, "shape");
	}
	
	@Test
	public void testRouteCollectionNotExists()
	{
		running
		(
			getFakeApplication(), 
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
	public void testCreationDateFormat(){
		running 		(
			getFakeApplication(), 
			new Runnable()			{
				public void run()				{
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					Result result = routeCreateDocument(getRouteAddress(sFakeCollection));
					assertRoute(result, "testRouteCMDDocument CREATE", Status.OK, null, true);
					String sCreationDate = getCreationDate();
					if (!sCreationDate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[\\+-]\\d{4}")) {
						 Assert.fail("_creationDate field is in wrong format: " + sCreationDate);
					}
				}
			}
		);
	}
	
	@Test
	public void testRouteCMDDocument()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					
					Result result = routeCreateDocument(getRouteAddress(sFakeCollection));
					assertRoute(result, "testRouteCMDDocument CREATE", Status.OK, null, true);
					String sRid = getRid();
					String sAuthor = getAuthor();
					Assert.assertTrue("_author field is not admin, found: " + sAuthor, sAuthor.equals("admin"));
			 		continueOnFail(true);
					
					try
					{
						// Test successful modify
						Result sucessModify = routeModifyDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
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
						Result badModify = routeModifyDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode("#1235:1", "ISO-8859-1"));
						assertRoute(badModify, "testRouteCMDDocument not existent RID. MODIFY RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Test modify with inconsistent RID
						result = routeModifyDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testServerCMDDocument bad RID. MODIFY RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID_MODIFY, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection
						result = routeGetDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Test the call_id feature
						result = routeGetDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode(sRid, "ISO-8859-1")+"?call_id=123");
						assertRoute(result, "testRouteCMDDocument.call_id", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
						assertJSONString(json, "\"call_id\":\"123\"");
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a not existent collection 
						result = routeGetDocument(SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument not existent collection get document RID <" + sRid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection using a not existent RID
						result = routeGetDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode("#1236:1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						result = routeGetDocument(getRouteAddress(sFakeCollection) + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document bad RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document2 RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection not existent RID
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode("#1237:1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document2 not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Mode 2 Retrieve document in a collection using invalid RID
						result = routeGetDocument(SERVICE_ROUTE + URLEncoder.encode("#1", "ISO-8859-1"));
						assertRoute(result, "testRouteCMDDocument get document2 bad RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					continueOnFail(false);
					
					// Delete document
					result = routeDeleteDocument(sFakeCollection, sRid);
					assertRoute(result, "testRouteCMDDocument DELETE RID <" + sRid + ">", Status.OK, null, false);
				}
			}
		);		
	}

	@Test
	public void testServerCollectionNotExists()
	{
		running
		(
			getTestServer(), 
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
	public void testAccessDocumentsWithoutAuth() {
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{

					//Anonymous user looking for a non existent collection
					FakeRequest request = new FakeRequest(GET, getRouteAddress("PIPPOPLUTO"));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					Result result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.not_exists", Status.NOT_FOUND, null, false);
					
					//Admin creates a collection					
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();		
					//Anonymous user looking for its documents
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.get_all", Status.OK, "\"result\":\"ok\",\"data\":[]", true);
					
					//Admin creates a document
					 result = routeCreateDocument(getRouteAddress(sFakeCollection));
					assertRoute(result, "testAccessDocumentsWithoutAuth,create", Status.OK, null, true);
					String sUUID = getUuid();
					
					//Anonymous user looking for it and grab jus an error 404
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/" + sUUID);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.not_exists", Status.NOT_FOUND, null, false);
					
					//Admin changes the grant to the document
					
					request = new FakeRequest(PUT, getRouteAddress(sFakeCollection) + "/" + sUUID + "/read/role/anonymous");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.grant", Status.OK, null, false);
					
					//Anonymous user looking for it and obtains it
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/" + sUUID);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.get_after_grant", Status.OK, null, false);	
					
					//Anonymous user looking for the collection and obtain a list with one record
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.get_all", Status.OK, "\"result\":\"ok\",\"data\":[{\"", true);

					//since the resource is now available to anonymous users, it should be visible to registered users too issue #195
					
					String fakeUsername=createNewUser("registeredUser");
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/" + sUUID);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(fakeUsername+":passw1"));
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsFromRegisteredUser", Status.OK, "\"result\":\"ok\",\"data\":{\"", true);
					
					//Admin revokes  the grant to the document
					request = new FakeRequest(DELETE, getRouteAddress(sFakeCollection) + "/" + sUUID + "/read/role/anonymous");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.revoke", Status.OK, null, false);
					
					//Anonymous user looking for it and obtains nothing
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/" + sUUID);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testAccessDocumentsWithoutAuth.not_found_2", Status.NOT_FOUND, null, false);	
					
				}
			}
		);		
	}
	
	@Test
	public void testServerCMDDocument()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeCollection = new AdminCollectionFunctionalTest().serverCreateCollection();

					serverCreateDocument(getURLAddress(sFakeCollection));
					assertServer("testServerCMDDocument CREATE", Status.OK, null, true);
					String sRid = getRid();
					String sUuid = getUuid();
					String sAuthor = getAuthor();
					Assert.assertTrue("_author field is not admin, found: " + sAuthor, sAuthor.equals("admin"));
					continueOnFail(true);
					
					try
					{
						// Test successful modify
						serverModifyDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument MODIFY RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
						
						// Test successful modify UUID
						serverModifyDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode(sUuid, "ISO-8859-1"));
						assertServer("testServerCMDDocument MODIFY UUID <" + sUuid + ">", Status.OK, null, true);
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
						
						// Test modify with non existent collection UUID
						serverModifyDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + COLLECTION_NOT_EXIST + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument no collection MODIFY UUID <" + sUuid + ">", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with non existent RID
						serverModifyDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode("#1238:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument not existent RID. MODIFY", Status.NOT_FOUND, null, false);
						
						// Test modify with non existent UUID
						serverModifyDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode("056e4b19-5c32-4d25-b70b-135e011d72a2", "ISO-8859-1"));
						assertServer("testServerCMDDocument not existent UUID. MODIFY", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Test modify with inconsistent RID
						serverModifyDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument bad RID. MODIFY RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID_MODIFY, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a collection
						serverGetDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument get document RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Retrieve document in a not existent collection
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
						serverGetDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode("#1239:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Retrieve document in a collection with not existent RID
						serverGetDocument(getURLAddress(sFakeCollection) + "/" + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document incosistent RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode(sRid, "ISO-8859-1"));
						assertServer("testServerCMDDocument get document2 RID <" + sRid + ">", Status.OK, null, true);
						assertJSONString(json, TEST_MODIFY_JSON);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					try
					{
						// Mode 2 Retrieve document in a collection not existent RID
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode("#1234:1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document2 not existent RID", Status.NOT_FOUND, null, false);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}

					try
					{
						// Mode 2 Retrieve document in a collection bad RID
						serverGetDocument(TestConfig.SERVER_URL + SERVICE_ROUTE + URLEncoder.encode("#1", "ISO-8859-1"));
						assertServer("testServerCMDDocument get document2 bad RID", Status.BAD_REQUEST, TestConfig.MSG_BAD_RID, true);
					}
					catch (UnsupportedEncodingException uex)
					{
						assertFail("Unexcpeted exception. " + uex.getMessage());
					}
					
					continueOnFail(false);

					// Delete created document
					serverDeleteDocument(sFakeCollection, sRid);
					assertServer("testServerCMDDocument DELETE RID <" + sRid + ">", Status.OK, null, false);
				}
	        }
		);
	}
	


	protected Result routeDeleteDocument(String sCollectionName, String sRid)
	{
		Result result = null;
		try
		{
			FakeRequest request = new FakeRequest(DELETE, getRouteAddress(sCollectionName) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"));
			request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
			request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
			result = routeAndCall(request);
		}
		catch (Exception ex)
		{
			Assert.fail("Unexcpeted exception. " + ex.getMessage());
		}
		
		return result;
	}
	
	protected void serverDeleteDocument(String sCollectionName, String sRid)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		try
		{
			httpRequest
			( 
		 		getURLAddress(sCollectionName) + "/" + URLEncoder.encode(sRid, "ISO-8859-1"),
				DELETE
			);
		}
		catch (Exception ex)
		{
			Assert.fail("Unexcpeted exception. " + ex.getMessage());
		}
	}
	
	private String getRid()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("@rid");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get RID value: " + ex.getMessage());
		}
		
		return sRet;
	}
	
	private String getUuid()
	{
		String sUuid = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		
		return sUuid;
	}
	

	private String getCreationDate()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_creation_date");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get _creation_date value: " + ex.getMessage());
		}
		
		return sRet;
	}
	
	private String getAuthor()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_author");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get _author value: " + ex.getMessage());
		}
		
		return sRet;
	}
}
