import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.Test;

import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import core.AbstractRouteHeaderTest;
import core.TestConfig;


public class PeopleTest extends AbstractRouteHeaderTest{

	public PeopleTest() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getRouteAddress() {
		return "/users";
	}

	@Override
	public String getMethod() {
		return "GET";
	}

	@Override
	protected void assertContent(String s) {
		Object json = toJSON(s);
		assertJSON(json, "user");
	}
	
	@Test
	public void testRoutePeopleBase()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "RouteOK Admin user", Status.OK, null, true);
					
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_DEFAULT_ENC);
					result = routeAndCall(request);
					assertRoute(result, "RouteOK BaasBox user", Status.FORBIDDEN, null, false);
				}
			}
		);		
	}

	@Test
	public void testRoutePeopleQuery()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress() + "?where=user.name like \"user%25\" and user.name.length() = 5 and visibleByFriend.key = \"value\"&orderBy=user.signUpDate asc");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "RouteOK Admin user", Status.OK, null, false);
					
				}
			}
		);		
	}
	
	@Test
	public void testRoutePeopleFields()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					FakeRequest request = new FakeRequest(getMethod(), getRouteAddress() + "?fields=user.name");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "RouteOK Admin user", Status.OK, null, false);
					
				}
			}
		);		
	}
	
}
