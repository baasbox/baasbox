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
import static play.test.Helpers.POST;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import junit.framework.Assert;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentCountFunctionalTest extends AbstractDocumentTest
{
	@Override
	public String getRouteAddress()
	{
		return SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME + "/count";
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
		assertJSONString(json, "count");
	}

	@Test 
	public void testRouteGetDocumentsCount()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					
					FakeRequest request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCount", Status.OK, null, true);
				}
			}
		);
	}
	
	@Test
	public void testServerGetDocumentsCount()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();

					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection) + "/count", GET);
					assertServer("testServerGetDocumentsCount", Status.OK, null, true);
				}
	        }
		);
	}
	
	
	@Test 
	public void testRouteGetDocumentsCountWithStar(){
		running	(
			getFakeApplication(), 
			new Runnable() 	{
				public void run() 	{
					//creates a fake collection
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					
					//creates a dummy user
					String sFakeUser = "UserTestCount_" + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

					// Create user
					FakeRequest request = new FakeRequest("POST","/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart -create fake user", 201, null, false);
					
					//insert 2 records using admin
					result=routeCreateDocument(getRouteAddress(sFakeCollection));
					result=routeCreateDocument(getRouteAddress(sFakeCollection));
					
					//execute a count(*) using admin, it should return 2
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 1", Status.OK, "\"data\":{\"count\":2}", true);
					
					//execute a count(*) using fake user, it should return 0
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count?where=1%3D1");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, "passw1"));
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 2 (workaround)", Status.OK, "\"data\":{\"count\":0}", true);

					//execute a count(*) using fake user, 
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, "passw1"));
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 3 (NO workaround)", Status.OK, "\"data\":{\"count\":0}", true);

					//insert a new document as fake user
					request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, "passw1"));
					request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
					result= routeAndCall(request); 
					assertRoute(result, "testRouteGetDocumentsCountWithStart 4", Status.OK, null, false);
					
					
					//execute a count(*) using admin, it should return 3
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 5", Status.OK, "\"data\":{\"count\":3}", true);
					
					//execute a count(*) using fake user, it should return 1
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, "passw1"));
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 6 (workaround)", Status.OK, "\"data\":{\"count\":1}", true);

					//execute a count(*) using fake user and a real where clause, it should return 1
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count?where=shape%3D%22square%22");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeUser, "passw1"));
					request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart 6a (workaround)", Status.OK, "\"data\":{\"count\":1}", true);	
				}
			}
		);
	}
	
	
	@Test 
	public void testRouteRawOrientDB(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
					//creates a fake collection
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					
					//creates a dummy user
					String sFakeUser = "UserTestCount_" + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

					// Create user
					FakeRequest request = new FakeRequest("POST","/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "testRouteGetDocumentsCountWithStart -create fake user", 201, null, false);
					
					//insert 2 records using admin
					result=routeCreateDocument(getRouteAddress(sFakeCollection));
					result=routeCreateDocument(getRouteAddress(sFakeCollection));

					try {
						DbHelper.open("1234567890",sFakeUser,"passw1");
					} catch (InvalidAppCodeException e) {
						assertFail("AppCode not valid(??!!!??) - " + ExceptionUtils.getMessage(e));
					}
					
					long count=0;
					try{
						List<ODocument> listOfdoc = (List<ODocument>) DbHelper.genericSQLStatementExecute("select count(*) from " + sFakeCollection,new String[]{});
						ODocument doc = listOfdoc.get(0);
						count=(Long)doc.field("count");
					}catch(Exception e){
						assertFail(ExceptionUtils.getMessage(e));
					}finally{
						if (DbHelper.getConnection() != null && !DbHelper.getConnection().isClosed()) DbHelper.getConnection().close();
					}
					
					//these two tests will fail when OrientDB will be patched, actually it will need to be swapped
						//this is WRONG, should not pass when OrientDB will be patched
						Assert.assertTrue("Finally OrientDB is patched", count==2);
						//this is wrong too
						Assert.assertFalse("Finally OrientDB is patched", count==0);
				}
			}
			);
	}
}
