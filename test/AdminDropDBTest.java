import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;


public class AdminDropDBTest extends AbstractTest{
	@Test
	public void testDeleteDB() throws Exception
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
					
					FakeRequest request = new FakeRequest("DELETE", "/admin/db/2000");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					Result result = routeAndCall(request);
					assertRoute(result, "testDelete", Status.OK, null, true);
				}
			}
			);
	}

	@Override
	public String getRouteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
}
