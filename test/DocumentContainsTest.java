import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;

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


public class DocumentContainsTest extends AbstractDocumentTest {
	private Object json = null;
	private JsonNode document1;
	private JsonNode jsonForUpdate;
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
	public void setDocument() throws JsonProcessingException, IOException{
		document1 = new ObjectMapper().readTree(
				"{\"players\": ["+
					"{"+
						"\"username\": \"XXXX@XXXX.COM\","+
						"\"name\": \"john doe\","+
						"\"points\": 0"+
					"},"+
					"{"+
					"\"username\": \"YYYY1@YYYY.COM\","+
					"\"name\": \"jane doe\","+
					"\"department\": \"one\","+
					"\"points\": 0"+
					"},"+
					"{"+
					"\"username\": \"ZZZZ@ZZZZ1.COM\","+
					"\"name\": \"dana scully\","+
					"\"department\": \"two\","+
					"\"points\": 0"+
					"}"+
				"],"+
				"\"status\": \"open\","+
				"\"player_turn\": 0,"+
				"\"id\": \"ff342a9b-79cd-4494-983e-6ec4a1a47c8a\","+
				"\"creationdate\": \"2014-02-15T13:49:51.051+0100\","+
				"\"_author\": \"admin\""
				+ "}");
	}
	
	@Test
	public void testContains(){
		running
		(
			getFakeApplication(), 
			new Runnable() 		{
				public void run() {
			 		continueOnFail(false);
					sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					//create  document
					FakeRequest request = new FakeRequest(POST, getRouteAddress(sFakeCollection));
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(document1,POST);
					Result result = routeAndCall(request); 
					assertRoute(result, "testContains CREATE 1", 200, null, true);
					String id1=getUuid();
	
					
					//read the doc
					request = new FakeRequest(GET, "/document/" + sFakeCollection + "/" + id1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testContains load document 1", Status.OK, "\"name\":\"john doe\"", true);
					
					
					//select it using the `contains`function
					request = new FakeRequest("GET", "/document/" + sFakeCollection +  "?where=players%20contains%20(username%3D%27XXXX%40XXXX.COM%27)");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "testContains load document 1 using contains", 200, "\"name\":\"john doe\"", true);					
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


