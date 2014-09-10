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
import static play.test.Helpers.PUT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentSerializationTest extends AbstractDocumentTest
{
	private final String ERROR_MESSAGE="JSON not valid. HINT: check if it is not just a JSON collection ([..]), a single element ({\\\"element\\\"}) or you are trying to pass a @version:null field";
	private Object json = null;
	private String collectionName;
	
	@Before
	public void createCollection()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					collectionName = new AdminCollectionFunctionalTest().routeCreateCollection();
				}
			}
		);		
	}
	
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
	}
	

	
	@Test
	public void testCreateWithVersionNull()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = createDocumentWithVersionNull(getRouteAddress(collectionName));
						assertRoute(result, "testCreateWithVersionNull CREATE", Status.BAD_REQUEST, ERROR_MESSAGE, true);
					}catch(Exception e){
						assertFail(e.getMessage());
					}
				}
			}
		);		
	}

	@Test
	public void createWithVersion()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = createDocumentWithVersion(getRouteAddress(collectionName));
						assertRoute(result, "createWithVersion CREATE", Status.BAD_REQUEST, "UpdateOldVersionException: Are you trying to create a document with a @version field?", true);
					}catch(Exception e){
						assertFail(e.getMessage());
					}
				}
			}
		);		
	}
	
	@Test
	public void modifyWithVersionNull()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = routeCreateDocument(getRouteAddress(collectionName));
						String uuid = getUuid(result);
						modifyDocumentWithVersionNUll(getRouteAddress(collectionName) + "/" + uuid);
						assertRoute(result, "modifyWithVersionNull MODIFY", Status.OK, null, false);
					}catch(Exception e){
						assertFail(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		);		
	}
	
	@Test
	public void testCreateDocumentWithJustCollection()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = createDocumentWithJustCollection(getRouteAddress(collectionName));
						assertRoute(result, "testCreateDocumentWithJustCollection CREATE", Status.BAD_REQUEST, ERROR_MESSAGE, true);
					}catch(Exception e){
						assertFail(e.getMessage());
					}
				}
			}
		);		
	}
	@Test
	public void testCreateDocumentWithJustOneElement()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = createDocumentWithJustOneElement(getRouteAddress(collectionName));
						assertRoute(result, "testCreateDocumentWithJustOneElement CREATE", Status.BAD_REQUEST, ERROR_MESSAGE, true);
					}catch(Exception e){
						assertFail(e.getMessage());
					}
				}
			}
		);		
	}
	
	@Test
	public void testCreateDocumentWithJustOneElementAsString()	{
		running	(getFakeApplication(), 	new Runnable() 	{
				public void run() {
					try{
						Result result = createDocumentWithJustOneElementAsString(getRouteAddress(collectionName));
						assertRoute(result, "testCreateDocumentWithJustOneElementAsString CREATE", Status.BAD_REQUEST, ERROR_MESSAGE, true);
					}catch(Exception e){
						assertFail(e.getMessage());
					}
				}
			}
		);		
	}
	
	protected Result createDocumentWithVersionNull(String sAddress) throws JsonProcessingException, IOException {
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("{\"k\":0,\"@version\":null}");
		request = request.withJsonBody(node);
		return routeAndCall(request); 
	}
	
	protected Result createDocumentWithJustCollection(String sAddress) throws JsonProcessingException, IOException {
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("[1,2]");
		request = request.withJsonBody(node);
		return routeAndCall(request); 
	}
	
	protected Result createDocumentWithJustOneElement(String sAddress) throws JsonProcessingException, IOException {
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("2");
		request = request.withJsonBody(node);
		return routeAndCall(request); 
	}
	
	protected Result createDocumentWithJustOneElementAsString(String sAddress) throws JsonProcessingException, IOException {
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("\"element\"");
		request = request.withJsonBody(node);
		return routeAndCall(request); 
	}

	protected Result createDocumentWithVersion(String sAddress) throws JsonProcessingException, IOException {
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("{\"k\":0,\"@version\":2}");
		request = request.withJsonBody(node);
		return routeAndCall(request); 
	}
	
	public Result modifyDocumentWithVersionNUll(String sAddress) throws JsonProcessingException, IOException {
		// Modify created document
		FakeRequest request = new FakeRequest(PUT, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		ObjectMapper om = new ObjectMapper();
		JsonNode node = om.readTree("{\"k\":0,\"@version\":12}");
		request = request.withJsonBody(node,PUT);
		return routeAndCall(request);
	}

	
	
	
}
