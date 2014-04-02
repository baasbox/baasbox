import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import static org.junit.Assert.*;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.configuration.Application;
import com.baasbox.db.DbHelper;

import core.AbstractAdminTest;
import core.TestConfig;


public class AdminSettingsModificationTest extends AbstractAdminTest{

	@Override
	public String getRouteAddress() {
		return "/admin/configuration/Application/application.name/fromquerystring";
	}
	
	public String getRouteAddressWithoutQS() {
		return "/admin/configuration/Application?subsection=dummy";
	}

	@Override
	public String getMethod() {
		return "PUT";
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
					
					String original = Application.APPLICATION_NAME.getValueAsString(); 
					//load settings
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "Set configuration with QS", OK, "data\":\"You provided key and value in the query string.In order to prevent security issue consider moving those value into the body of the request.\"", true);
					
					//Verify value has changed
					request = new FakeRequest("GET", "/admin/configuration/dump.json");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON", OK, 
							"application.name\":\"fromquerystring\"", true);
				
					
					//Write value with body
					request = new FakeRequest(getMethod(), getRouteAddressWithoutQS());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					JsonFactory factory = new JsonFactory();
					ObjectMapper mp = new ObjectMapper(factory);
					JsonNode node = null;
					try{
						node = mp.readTree("{\"key\":\"application.name\",\"value\":\"frombodyparams\"}");
					}catch(Exception e){
						fail();
					}
					request = request.withJsonBody(node);
					result = routeAndCall(request);
					assertRoute(result, "Set configuration with Request Body", OK, "", false);
					
					request = new FakeRequest("GET", "/admin/configuration/dump.json");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON", OK, 
							"application.name\":\"frombodyparams\"", true);
				
					//Write value with body
					request = new FakeRequest(getMethod(), getRouteAddressWithoutQS());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					try{
						node = mp.readTree("{\"key\":\"application.name\",\"value\":\""+original+"\"}");
					}catch(Exception e){
						fail();
					}
					request = request.withJsonBody(node);
					result = routeAndCall(request);
					assertRoute(result, "Set configuration with Request Body", OK, "", false);
					
				}
			}
		);		
	}
	
	
	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
	

}
