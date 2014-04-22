import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import play.Logger;
import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractAdminTest;
import core.AbstractUserTest;
import core.TestConfig;


public class AdminUserChangePasswordTest extends AbstractUserTest {

	@Override
	public String getRouteAddress()
	{
		return "/admin/user/";
	}
	
	@Override 
	public String getMethod()
	{
		return PUT;
	}
	
	@Override
	protected String getURLAddress()
	{
		return TestConfig.SERVER_URL + getRouteAddress();		
	}
	
	@Override
	protected void assertContent(String s)
	{
	}
	
	@Override
	public String getDefaultPayload()
	{
		return "/adminUserChangePasswordPayload.json";
	}

	@Test
	public void testRouteChangePassword()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
					
					// Test change password
					String route=getRouteAddress()+sFakeUser+"/password";
					FakeRequest request = new FakeRequest(getMethod(), route);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserChangePasswordPayload.json"), getMethod());
					Result result = routeAndCall(request);
					if (Logger.isDebugEnabled()) Logger.debug("testRouteChangePassword request: " + request.getWrappedRequest().headers());
					if (Logger.isDebugEnabled()) Logger.debug("testRouteChangePassword result: " + contentAsString(result));
					assertRoute(result, "testRouteChangePassword 1", Status.OK, null, false);
					
					continueOnFail(true);
					
					// Test change password non valid
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserChangePasswordInvalid.json"), getMethod());
					result = routeAndCall(request);
					assertRoute(result, "testRouteChangePassword 2 not valid", Status.BAD_REQUEST, TestConfig.MSG_CHANGE_ADMIN_PWD, true);

					continueOnFail(false);
					
					// Restore old password
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(getPayload("/adminUserRestorePasswordPayload.json"), getMethod());
					result = routeAndCall(request);
					assertRoute(result, "testRouteChangePassword 3 restore old password", Status.OK, null, false);
				}
			}
		);		
	}

	@Override
	public void testRouteNotValid() {
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
					String route=getRouteAddress()+sFakeUser+"/password";

					// No AppCode, No Authorization
					FakeRequest request = new FakeRequest(getMethod(), route);
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
					request = new FakeRequest(getMethod(), route);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
				}
			}
		);		
	}
	@Override
	public void testServerNotValid() {
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
					String route=getURLAddress()+sFakeUser+"/password";
					
					// No AppCode, No Authorization
					removeAllHeaders();
					httpRequest(route, getMethod(), getDefaultPayload());
					assertServer("No AppCode, No Authorization", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE_NO_AUTH, true);
					
					// No Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					httpRequest(route, getMethod(), getDefaultPayload());
					assertServer("No Authorization", UNAUTHORIZED, null, false);
					
					// Invalid AppCode
					setHeader(TestConfig.KEY_APPCODE, "1");
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(route, getMethod(), getDefaultPayload());
					assertServer("Invalid AppCode", UNAUTHORIZED, TestConfig.MSG_INVALID_APP_CODE, true);
	
					// Invalid Authorization
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, "Basic dXNlcjE6cGFzc3c=");
					httpRequest(route, getMethod(), getDefaultPayload());
					assertServer("Invalid Autorization", UNAUTHORIZED, null, false);
	
					// No AppCode
					removeHeader(TestConfig.KEY_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					httpRequest(route, getMethod(), getDefaultPayload());
					assertServer("No AppCode", BAD_REQUEST, TestConfig.MSG_NO_APP_CODE, true);
	            }
	        }
		);
	}

}
