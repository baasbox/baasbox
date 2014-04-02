import static org.junit.Assert.fail;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.Iterator;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractAdminTest;
import core.TestConfig;


public class AdminSettingsModificationTest extends AbstractAdminTest{

	String originalValue;
	final static String BASE_URL = "/admin/configuration/";

	@Override
	public String getRouteAddress() {
		return BASE_URL + "dump.json";
	}

	public String getRouteAddress(String path) {
		return BASE_URL + path;
	}


	public String getRouteAddressWithoutQS() {
		return getRouteAddress("Application/application.name");
	}

	@Override
	public String getMethod() {
		return GET;
	}




	@Test 
	public void test()
	{
		running
		(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						JsonFactory factory = new JsonFactory();
						ObjectMapper mp = new ObjectMapper(factory);


						FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						Result result = routeAndCall(request);

						JsonNode configuration = null;
						try{
							configuration = mp.readTree(contentAsString(result));
						}catch(Exception e){

						}

						originalValue = findInConfigurationDump(configuration,"Application","application","application.name");
						Assert.assertNotNull("Original application.name value not found", originalValue);


						//load settings
						request = new FakeRequest(PUT, getRouteAddress("Application/application.name/fromquerystring"));
						request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						result = routeAndCall(request);
						assertRoute(result, "Set configuration with QS", OK, "data\":\"You provided key and value in the query string.In order to prevent security issue consider moving those value into the body of the request.\"", true);

						//Verify value has changed
						request = new FakeRequest(getMethod(), getRouteAddress());
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						result = routeAndCall(request);
						assertRoute(result, "LoadConfigurationAsJSON", OK, 
								"application.name\":\"fromquerystring\"", true);


						//Write value with body
						request = new FakeRequest(PUT, getRouteAddressWithoutQS());
						request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						JsonNode node = null;
						try{
							node = mp.readTree("{\"value\":\"frombodyparams\"}");
						}catch(Exception e){
							fail("Unable to parse");
						}

						request = request.withJsonBody(node,PUT);
						result = routeAndCall(request);
						assertRoute(result, "Set configuration with Request Body", OK, null, false);

						request = new FakeRequest("GET", "/admin/configuration/dump.json");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						result = routeAndCall(request);
						assertRoute(result, "LoadConfigurationAsJSON", OK, 
								"application.name\":\"frombodyparams\"", true);


						request = new FakeRequest(PUT, getRouteAddressWithoutQS());
						request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						node = null;
						try{
							node = mp.readTree("{\"value\":\""+originalValue+"\"}");
						}catch(Exception e){
							fail("Unable to parse");
						}

						request = request.withJsonBody(node,PUT);
						result = routeAndCall(request);
						assertRoute(result, "Set Original configuration with Request Body", OK, null, false);

					}


				}
				);		
	}

	private String findInConfigurationDump(JsonNode data,
			String section,String subsection, String key) {
		Iterator<JsonNode> values = data.get("data").getElements();
		String result = null;
		while(values.hasNext()){
			JsonNode n = values.next();

			if(n.has("section") && n.get("section").getTextValue().equalsIgnoreCase(section)){
				JsonNode subValues = n.get("sub sections").get(subsection);

				if(subValues!= null){
					Iterator<JsonNode> keys = subValues.getElements();
					while(keys.hasNext()){
						JsonNode keyNode = keys.next();
						if(keyNode.has(key)){
							result = keyNode.get(key).getTextValue();
							break;
						}
					}
				}
			}

		}
		return result;

	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub

	}



}
