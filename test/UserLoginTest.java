import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.security.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractTest;
import core.TestConfig;


public class UserLoginTest extends AbstractTest
{

	private static final String ADMIN_USERNAME = TestConfig.AUTH_ADMIN.split(":")[0];
	private static final String ADMIN_PASSWORD = TestConfig.AUTH_ADMIN.split(":")[1];
	private Object json;
	@Override
	public String getRouteAddress() {
		// TODO Auto-generated method stub
		return "/login";
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return POST;
	}

	@Override
	protected void assertContent(String s) {
		json = toJSON(s);
		assertJSON(json, "user");
		
	}
	
	
	
	
	@Test
	public void testRouteLoginUser()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
					
					// Test login user
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					Map<String,String> params = new HashMap<String,String>();
					params.put("username",ADMIN_USERNAME);
					params.put("password",ADMIN_PASSWORD);
					params.put("appcode",TestConfig.VALUE_APPCODE);
					request = request.withFormUrlEncodedBody(params);
					Result result = routeAndCall(request);
					
					assertRoute(result, "testRouteLoginUser", Status.OK, null, true);
					String body = play.test.Helpers.contentAsString(result);
					
					JsonNode jsonRes = Json.parse(body);
					String token = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					Assert.assertNotNull(token);
					JsonNode user = jsonRes.get("data").get("user");
					Assert.assertNotNull(user);
					assertJSON(user, "admin");
					
					//test logout
					request = new FakeRequest(POST, "/logout");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_TOKEN, token);
					result = routeAndCall(request);
					assertRoute(result, "testRouteLogoutUser", Status.OK, "\"result\":\"ok\",\"data\":\"user logged out\",\"http_code\":200", true);
					
				}
			}
		);		
	}


	@Test
	public void testRouteLoginUserJson()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					
					// Test login user
					ObjectMapper om = new ObjectMapper();
					JsonNode payload;
					try {
						payload = om.readTree("{\"username\":\""+ADMIN_USERNAME+"\",\"password\":\""+ADMIN_PASSWORD+"\",\"appcode\":\""+TestConfig.VALUE_APPCODE+"\"}");
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace((e)));
						return;
					}
					
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
					httpRequest	( 
							TestConfig.SERVER_URL + getRouteAddress(),
							getMethod(),
							payload
					);
					assertServer("testServerLoginUserJson", Status.OK, null, false);
					
					String body = getResponse();
					
					JsonNode jsonRes = Json.parse(body);
					String token = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					Assert.assertNotNull(token);
					JsonNode user = jsonRes.get("data").get("user");
					Assert.assertNotNull(user);
					assertJSON(user, "admin");
					
					//test logout
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_TOKEN, token);
					removeHeader(HttpHeaders.CONTENT_TYPE);
					httpRequest	( 
							TestConfig.SERVER_URL + "/logout",
							POST
					);
					
					assertServer("testServerLogoutUser", Status.OK, "\"result\":\"ok\",\"data\":\"user logged out\",\"http_code\":200", true);
					
				}
			}
		);		
	}

	
	@Test
	public void testRouteLoginUserPlainText()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					
					ObjectMapper om = new ObjectMapper();
					JsonNode payload;
					try {
						payload = om.readTree("{\"username\":\""+ADMIN_USERNAME+"\",\"password\":\""+ADMIN_PASSWORD+"\",\"appcode\":\""+TestConfig.VALUE_APPCODE+"\"}");
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace((e)));
						return;
					}
					
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
					httpRequest	( 
							TestConfig.SERVER_URL + getRouteAddress(),
							getMethod(),
							payload
					);
					assertServer("testServerLoginUserPlainText", Status.BAD_REQUEST, "Detected: text/plain", true);
				}
			}
		);		
	}	
}
