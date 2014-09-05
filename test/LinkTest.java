/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.GET;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;

public class LinkTest extends AbstractTest {

	private String collection2;
	private String collection1;
	private String username1;
	private String password1;
	private String username2;
	private String password2;
	private String idLink;
	private String idLinkByUser;

	@Override
	public String getRouteAddress() {
		return "/link";
	}

	public String getRouteCreateLink(String idSource, String idDest,
			String linkName) {
		return getRouteAddress() + "/" + idSource + "/" + linkName + "/"
				+ idDest;
	}

	public String getLinkById(String idLink) {
		return getRouteAddress() + "/" + idLink;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub

	}

	@Before
	public void prepareTest() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				// create a collection for test
				// Admin creates two collections
				collection1 = new AdminCollectionFunctionalTest()
						.routeCreateCollection();
				collection2 = new AdminCollectionFunctionalTest()
						.routeCreateCollection();

				// let's create two users
				username1 = UUID.randomUUID().toString();
				password1 = "passw1"; // defined in adminUserCreatePayload.json
				JsonNode node = updatePayloadFieldValue(
						"/adminUserCreatePayload.json", "username", username1);
				FakeRequest request = new FakeRequest(POST, "/user");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withJsonBody(node, POST);
				Result result = routeAndCall(request);
				contentAsString(result);

				username2 = UUID.randomUUID().toString();
				password2 = "passw1"; // defined in adminUserCreatePayload.json
				node = updatePayloadFieldValue("/adminUserCreatePayload.json",
						"username", username2);
				request = new FakeRequest(POST, "/user");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withJsonBody(node, "POST");
				result = routeAndCall(request);
				contentAsString(result);
			}
		});
	}

	@Test
	public void t1_testServerLink() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				continueOnFail(false);
				String idSource = "";
				String idDest = "";
				Result result=null;
				FakeRequest request = null;
				
					// administrator creates two documents
					idSource = createDocumentAsAdmin(collection1);
					idDest = createDocumentAsAdmin(collection2);
	
					// administrator links the two documents with the "has" link
					// name
					result = createLink(TestConfig.ADMIN_USERNAME,
							TestConfig.AUTH_ADMIN_PASS, idSource, idDest, "has");
					assertRoute(result, "LinkTest CREATE by Admin", Status.OK,
							null, true);

					idLink = getIdFromDocument(result);
					Assert.assertNotNull("id not found", idLink);
					String author = getAuthorFromDocument(result);
					Assert.assertTrue("author is not admin",
							author.equals(TestConfig.ADMIN_USERNAME));
					String audit = getAuditFromDocument(result);
					Assert.assertNull("audit is null", audit);
				try{
					// user1 tries to connect two docs, but he has not access to
					// them
					result = createLink(username1, password1, idSource, idDest,
							"has");
				}catch(Throwable e){
					assertFail("Create Link as registerd user: " + ExceptionUtils.getStackTrace(e));
				}
					assertRoute(result,
							"LinkTest CREATE by User 1 - first attempt",
							Status.BAD_REQUEST, null, true);

					// admin grants read permission on source
					request = new FakeRequest(PUT, "/document/"
							+ collection1 + "/" + idSource + "/read/user/"
							+ username1);
					request = request.withHeader(TestConfig.KEY_APPCODE,
							TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,
							TestConfig.AUTH_ADMIN_ENC);
					request = request.withHeader(HttpHeaders.CONTENT_TYPE,
							MediaType.APPLICATION_FORM_URLENCODED);
					result = routeAndCall(request);
					contentAsString(result);
	
					// user1 tries to connect two docs, but he has not access to
					// dest
					result = createLink(username1, password1, idSource, idDest,
							"has");
					assertRoute(result,
							"LinkTest CREATE by User 1 - second attempt",
							Status.BAD_REQUEST, null, true);
	
					// admin grants read permission on destination
					request = new FakeRequest(PUT, "/document/" + collection2 + "/"	+ idDest + "/read/user/" + username1);
					request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					contentAsString(result);
	
					// user1 tries to connect two docs, and everything goes ok
					result = createLink(username1, password1, idSource, idDest,	"has");
					assertRoute(result,	"LinkTest CREATE by User 1 - third attempt", Status.OK,	null, true);
	
					idLinkByUser = getIdFromDocument(result);
					continueOnFail(true);
					
					// try to retrieve a non-existent link
					result = getLink(TestConfig.ADMIN_USERNAME,TestConfig.AUTH_ADMIN_PASS, "mango"); // :-)
					assertRoute(result, "Get By fake id 1", 404, null, false);
					// administrator can read both links and their node
					result = getLink(TestConfig.ADMIN_USERNAME,TestConfig.AUTH_ADMIN_PASS, idLink);
					assertRoute(result, "Get By id 1", 200, "\"@class\":\""+collection1, true);
					assertRoute(result, "Get By id 1a", 200, "\"@class\":\""+collection2, true);
					assertRoute(result, "Get By id 1b", 200, "\"id\":\""+idLink, true);
					
					// user1 can read both links and their node
					result = getLink(username1,password1, idLink);
					assertRoute(result, "Get By id 2", 200, "\"@class\":\""+collection1, true);
					assertRoute(result, "Get By id 2a", 200, "\"@class\":\""+collection2, true);
					assertRoute(result, "Get By id 2b", 200, "\"id\":\""+idLink, true);
					
					result = getLink(username1,password1, idLinkByUser);
					assertRoute(result, "Get By id 3", 200, "\"@class\":\""+collection1, true);
					assertRoute(result, "Get By id 3a", 200, "\"@class\":\""+collection2, true);
					assertRoute(result, "Get By id 3b", 200, "\"id\":\""+idLinkByUser, true);			
					
					//admin revoke read permission on destId
					request = new FakeRequest(DELETE, "/document/" + collection2 + "/"	+ idDest + "/read/user/" + username1);
					request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					contentAsString(result);		
					
					//user1 cannot read the link
					result = getLink(username1,password1, idLinkByUser);
					assertRoute(result, "Get By id 4", 200, "\"@class\":\""+collection1, true);
					assertRoute(result, "Get By id 4a", 200, "\"in\":null", true);
					assertRoute(result, "Get By id 4b", 200, "\"id\":\""+idLinkByUser, true);					
				
			}

		});
	}// testServerCreateLink



	public String createDocumentAsAdmin(String collection) {
		FakeRequest request = new FakeRequest(POST,
				DocumentCMDFunctionalTest.getRouteAddress(collection));
		request = request.withHeader(TestConfig.KEY_APPCODE,
				TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH,
				TestConfig.AUTH_ADMIN_ENC);
		request = request
				.withJsonBody(getPayload("/documentCreatePayload.json"));
		Result result = routeAndCall(request);
		return getIdFromDocument(result);
	}

	public String createDocumentAsUser(String username, String password,
			String collection) {
		FakeRequest request = new FakeRequest(POST, "/document/" + collection);
		request = request.withHeader(TestConfig.KEY_APPCODE,
				TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH,
				TestConfig.encodeAuth(username, password));
		request = request
				.withJsonBody(getPayload("/documentCreatePayload.json"));
		Result result = routeAndCall(request);
		return getIdFromDocument(result);
	}

	public Result createLink(String username, String password, String idSource,
			String idDest, String linkName) {
		FakeRequest request = new FakeRequest(POST, getRouteCreateLink(
				idSource, idDest, linkName));
		request = request.withHeader(TestConfig.KEY_APPCODE,
				TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH,
				TestConfig.encodeAuth(username, password));
		Result result = routeAndCall(request);
		return result;
	}

	public Result getLink(String username, String password, String linkId) {
		FakeRequest request = new FakeRequest(GET, getLinkById(linkId));
		request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username, password));
		Result result = routeAndCall(request);
		return result;
	}

	private String getIdFromDocument(Result result) {
		String sUuid = null;
		Object json = null;
		String content = contentAsString(result);
		try {
			json = toJSON(content);
			JSONObject jo = (JSONObject) json;
			sUuid = jo.getJSONObject("data").getString("id");
		} catch (Exception ex) {
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage()
					+ "\n The json object is: \n" + json);
		}

		return sUuid;
	}

	private String getAuthorFromDocument(Result result) {
		String author = null;
		Object json = null;
		String content = contentAsString(result);
		try {
			json = toJSON(content);
			JSONObject jo = (JSONObject) json;
			author = jo.getJSONObject("data").getString("_author");
		} catch (Exception ex) {
			Assert.fail("Cannot get _author  value: " + ex.getMessage()
					+ "\n The json object is: \n" + json);
		}

		return author;
	}

	private String getAuditFromDocument(Result result) {
		String audit = null;
		Object json = null;
		String content = contentAsString(result);
		try {
			json = toJSON(content);
			JSONObject jo = (JSONObject) json;
			audit = jo.getJSONObject("data").getString("_audit");
		} catch (Exception ex) {
		}

		return audit;
	}

}
