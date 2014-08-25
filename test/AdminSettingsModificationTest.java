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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
		return getRouteAddress("Push/profile1.sandbox.ios.certificate.password");
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

						originalValue = findInConfigurationDump(configuration,"Push","push","profile1.sandbox.ios.certificate.password");
						


						//load settings
						request = new FakeRequest(PUT, getRouteAddress("Push/profile1.sandbox.ios.certificate.password/fromquerystring"));
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
								"profile1.sandbox.ios.certificate.password\":\"fromquerystring\"", true);


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
								"profile1.sandbox.ios.certificate.password\":\"frombodyparams\"", true);


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
		Iterator<JsonNode> values = data.get("data").elements();
		String result = null;
		while(values.hasNext()){
			JsonNode n = values.next();

			if(n.has("section") && n.get("section").textValue().equalsIgnoreCase(section)){
				JsonNode subValues = n.get("sub sections").get(subsection);

				if(subValues!= null){
					Iterator<JsonNode> keys = subValues.elements();
					while(keys.hasNext()){
						JsonNode keyNode = keys.next();
						if(keyNode.has(key)){
							result = keyNode.get(key).textValue();
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
