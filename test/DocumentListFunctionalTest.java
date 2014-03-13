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

import org.apache.http.HttpHeaders;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;


public class DocumentListFunctionalTest extends AbstractDocumentTest
{
	@Override
	public String getRouteAddress()
	{
		return DocumentCMDFunctionalTest.SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
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
	}

	@Test 
	public void testRouteListDocuments()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
				
					// Test list documents in empty collection
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteListDocuments empty collection", Status.OK, null, false);
					
					result = routeCreateDocument(getRouteAddress(sFakeCollection));
					assertRoute(result, "testRouteListDocuments CREATE document in fake collection", Status.OK, null, true);
					
					// Test list documents in empty collection
					request = new FakeRequest(getMethod(), getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteListDocuments not empty collection", Status.OK, null, true);
				}
			}
		);
	}
	
	@Test
	public void testRouteListDocumentsBadCollection()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), SERVICE_ROUTE + COLLECTION_NOT_EXIST);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteListDocumentsBadCollection", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
				}
			}
		);
	}
	
	@Test
	public void testServerListDocuments()
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
					
					// Test list documents in empty collection
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection), getMethod());
					assertServer("testServerListDocuments empty collection", Status.OK, null, false);
					
					serverCreateDocument(getURLAddress(sFakeCollection));
					assertServer("testServerListDocuments CREATE document in fake collection", Status.OK, null, true);

					// Test list documents in not empty collection
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection), getMethod());
					assertServer("testServerListDocuments not empty collection", Status.OK, null, true);
				}
	        }
		);
	}

	@Test
	public void testServerListDocumentsBadCollection()
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
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest
					( 
						TestConfig.SERVER_URL + SERVICE_ROUTE + COLLECTION_NOT_EXIST,
						getMethod()
					);
					assertServer("testServerListDocumentsBadCollection", Status.NOT_FOUND, TestConfig.MSG_INVALID_COLLECTION, true);
				}
	        }
		);
	}
}
