package core;

import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

public abstract class AbstractRouteHeaderTest extends AbstractTest {



	public AbstractRouteHeaderTest() {
		super();
	}

	@Test
	public void testRouteNotValid() {
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// No AppCode, No Authorization
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					Result result = routeAndCall(request);
					assertRoute(result, "No AppCode No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE_NO_AUTH, true);
	
					// No Authorization
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					result = routeAndCall(request);
					assertRoute(result, "No Authorization", UNAUTHORIZED, null, false);
					
					// Invalid AppCode
					request = request.withHeader(TestConfig.KEY_APPCODE, "12345890");
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	
					// Invalid Authorization
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					result = routeAndCall(request);
					assertRoute(result, "Invalid Authorization", UNAUTHORIZED, null, false);
					
					// No AppCode
					request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
				}
			}
		);		
	}

	@Test
	public void testServerNotValid() {
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					// No AppCode, No Authorization
					removeAllHeaders();
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode, No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE_NO_AUTH, true);
					
					// No Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No Authorization", UNAUTHORIZED, null, false);
					
					// Invalid AppCode
					setHeader(TestConfig.KEY_APPCODE, "1");
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	
					// Invalid Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("Invalid Autorization", UNAUTHORIZED, null, false);
	
					// No AppCode
					removeHeader(TestConfig.KEY_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(getURLAddress(), getMethod(), getDefaultPayload());
					assertServer("No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
	            }
	        }
		);
	}

}