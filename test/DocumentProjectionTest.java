import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import core.AbstractDocumentTest;
import core.TestConfig;


public class DocumentProjectionTest extends AbstractDocumentTest {
	private Object json = null;
	private JsonNode document1;
	private JsonNode document2;
	private JsonNode document3;
	private JsonNode document4;
	
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
	public void createCollection() throws JsonProcessingException, IOException{
		document1 = new ObjectMapper().readTree("{\"total\":2,\"city\":\"rome\"}");
		document2 = new ObjectMapper().readTree("{\"total\":3,\"city\":\"rome\"}");
		document3 = new ObjectMapper().readTree("{\"total\":4,\"city\":\"milan\"}");
		document4 = new ObjectMapper().readTree("{\"total\":\"5\",\"city\":\"milan\"}");
	}
	
	@Test
	public void testOnlyFields(){
		running
		(
			getFakeApplication(), 
			new Runnable() 		{
				public void run() {
			 		continueOnFail(false);
					sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					//create three different documents
					FakeRequest request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(document1);
					Result result = routeAndCall(request); 
					assertRoute(result, "testOnlyFields CREATE 1", Status.OK, null, true);
					String id1=getUuid();
					
					request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(document2);
					result = routeAndCall(request); 
					assertRoute(result, "testOnlyFields CREATE 2", Status.OK, null, true);
					String id2=getUuid();
					
					request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(document3);
					result = routeAndCall(request); 
					assertRoute(result, "testOnlyFields CREATE 3", Status.OK, null, true);
					String id3=getUuid();

					request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(document4);
					result = routeAndCall(request); 
					assertRoute(result, "testOnlyFields CREATE 4", Status.OK, null, true);
					String id4=getUuid();
					
					//read one document
					request = new FakeRequest(GET, "/document/" + sFakeCollection + "/" + id1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testOnlyFields Reload document 1", Status.OK, "\"total\":2,\"city\":\"rome\"", true);
					
					//retrieve fields with aggregate functions amd group by clause
					request = new FakeRequest(GET, "/document/" + sFakeCollection + "?fields=sum(total) as tot,city&groupBy=city&orderBy=city");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testOnlyFields fields 1", Status.OK, "\"tot\":5,\"city\":\"rome\"", true);
					assertRoute(result, "testOnlyFields fields 2", Status.OK, "\"tot\":4,\"city\":\"milan\"", true);					

					
					request = new FakeRequest(GET, "/document/" + sFakeCollection + "?fields=sum(eval(\"(total - 10) * 2\")) as tot,city&groupBy=city&orderBy=city");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testOnlyFields fields 3", Status.OK, "\"tot\":-30,\"city\":\"rome\"", true);
					
					//one total field is a string, to include its value we must use the eval() function
					request = new FakeRequest(GET, "/document/" + sFakeCollection + "?fields=sum(eval(\"total * 1\")) as tot,city&groupBy=city&orderBy=city");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testOnlyFields fields 4", Status.OK, "\"tot\":5,\"city\":\"rome\"", true);
					assertRoute(result, "testOnlyFields fields 5", Status.OK, "\"tot\":9,\"city\":\"milan\"", true);					

					
					//delete collection
					request = new FakeRequest(DELETE, "/admin/collection/" + sFakeCollection );
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testOnlyFields delete", Status.OK, null, false);					
					
				}
			}
		);			
	}
	
	private String getUuid()	{
		String sUuid = null;

		try	{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}
		catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
	}
}


