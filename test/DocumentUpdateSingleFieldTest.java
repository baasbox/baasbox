import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentUpdateSingleFieldTest extends AbstractDocumentTest {
	private Object json = null;
	private JsonNode document1;
	private JsonNode jsonForUpdate;
	private JsonNode jsonForArrayUpdate, jsonForArrayIndexUpdate,
			jsonForArrayObjectUpdate, jsonForObjectStatusUpdate;
	private String sFakeCollection;

	@Override
	public String getRouteAddress() {
		return SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
	}

	@Override
	public String getMethod() {
		return GET;
	}

	@Override
	protected void assertContent(String s) {
		json = toJSON(s);
		assertJSON(json, "@version");
	}

	@Before
	public void setDocument() throws JsonProcessingException, IOException {
		document1 = new ObjectMapper()
				.readTree("{\"total\":2,\"city\":\"rome\"}");
		jsonForUpdate = new ObjectMapper().readTree("{\"data\":\"milan\"}");
		jsonForArrayUpdate = new ObjectMapper()
				.readTree("{\"data\":[\"one\",\"two\",\"three\"]}");
		jsonForArrayIndexUpdate = new ObjectMapper()
				.readTree("{\"data\":\"four\"}");
		jsonForArrayObjectUpdate = new ObjectMapper()
				.readTree("{\"data\":[{\"title\":\"issue1\",\"description\":\"issue1-desc\",\"tags\":[\"1\",\"2\",\"3\"]}]}");
		jsonForObjectStatusUpdate = new ObjectMapper()
				.readTree("{\"data\":\"ONGOING\"}");

	}

	@Test
	public void testOnlyFields() throws Exception {
		running(getFakeApplication(), new Runnable() {
			public void run() {
				continueOnFail(false);
				sFakeCollection = new AdminCollectionFunctionalTest()
						.routeCreateCollection();
				// create document
				FakeRequest request = new FakeRequest(POST,
						getRouteAddress(sFakeCollection));
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request = request.withJsonBody(document1, POST);
				Result result = routeAndCall(request);
				assertRoute(result, "testOnlyFields CREATE 1", 200, null, true);
				String id1 = getUuid();

				// read the doc
				request = new FakeRequest(GET, "/document/" + sFakeCollection
						+ "/" + id1);
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields load document 1",
						Status.OK, "\"total\":2,\"city\":\"rome\"", true);

				// update one fields
				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.city");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request = request.withJsonBody(jsonForUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 1", Status.OK,
						"\"city\":\"milan\"", true);

				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.tags");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request = request.withJsonBody(jsonForArrayUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 2", Status.OK,
						"\"tags\":[\"one\",\"two\",\"three\"]", true);

				request = new FakeRequest("GET", "/document/" + sFakeCollection
						+ "/" + id1 + "/.tags%5B0%5D");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 3", Status.OK,
						"\"result\":\"one\"", true);

				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.tags%5B3%5D");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request.withJsonBody(jsonForArrayIndexUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 4", Status.OK,
						"\"tags\":[\"one\",\"two\",\"three\",\"four\"]", true);

				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.issues");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request.withJsonBody(jsonForArrayObjectUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 5", Status.OK,
						"\"tags\":[\"1\",\"2\",\"3\"]", true);

				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.issues%5B0%5D/.status");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request.withJsonBody(jsonForObjectStatusUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 6", Status.OK,
						"\"status\":\"ONGOING\"", true);

				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.issues%5B0%5D/.tags%5B3%5D");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request.withJsonBody(jsonForArrayIndexUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 7", Status.OK,
						"\"tags\":[\"1\",\"2\",\"3\",\"four\"]", true);

				// issue #243
				// create a new user
				String user = routeCreateNewUser("user1-");
				// give him the read permission
				request = new FakeRequest(PUT, "/document/" + sFakeCollection
						+ "/" + id1 + "/read/user/" + user);
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				request = request.withHeader(HttpHeaders.CONTENT_TYPE,
						MediaType.APPLICATION_FORM_URLENCODED);
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields.grant", Status.OK, null,
						false);

				// try to update a field
				request = new FakeRequest("PUT", "/document/" + sFakeCollection
						+ "/" + id1 + "/.city");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.encodeAuth(user, "passw1"));
				request = request.withJsonBody(jsonForUpdate, "PUT");
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields fields 1", 403,
						"You have not the right to modify the document", true);

				// check if the password is shown
				request = new FakeRequest(GET, getRouteAddress(sFakeCollection)
						+ "?fields=expand(_allowRead)%20as%20ar");
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.encodeAuth(user, "passw1"));
				result = routeAndCall(request);
				String sContent = contentAsString(result);
				Assert.assertTrue("testOnlyFields fields 2: " + sContent,!sContent.contains("ord\":\""));

				// delete collection
				request = new FakeRequest(DELETE, "/admin/collection/"
						+ sFakeCollection);
				request = request.withHeader(TestConfig.KEY_APPCODE,
						TestConfig.VALUE_APPCODE);
				request = request.withHeader(TestConfig.KEY_AUTH,
						TestConfig.AUTH_ADMIN_ENC);
				result = routeAndCall(request);
				assertRoute(result, "testOnlyFields delete", Status.OK, null,
						false);

			}
		});
	}

	public String routeCreateNewUser(String username) {
		String sFakeUser = username + "-" + UUID.randomUUID();
		// Prepare test user
		JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json",
				"username", sFakeUser);

		// Create user
		FakeRequest request = new FakeRequest(POST, "/user");
		request = request.withHeader(TestConfig.KEY_APPCODE,
				TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH,
				TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(node, POST);
		Result result = routeAndCall(request);
		assertRoute(result, "Create user.", Status.CREATED, null, false);

		return sFakeUser;
	}

	private String getUuid() {
		String sUuid = null;

		try {
			JSONObject jo = (JSONObject) json;
			sUuid = jo.getJSONObject("data").getString("id");
		} catch (Exception ex) {
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage()
					+ "\n The json object is: \n" + json);
		}
		return sUuid;
	}
}
