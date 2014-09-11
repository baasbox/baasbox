import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.HashMap;

import org.junit.Before;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.TestConfig;


public class PushProfileTestDBNew extends PushProfileAbstractTest {

	public PushProfileTestDBNew() {}

	@Before
	public void beforeTest(){
		//import db
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
					FakeRequest request = new FakeRequest("DELETE", "/admin/db/100");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					Result result = routeAndCall(request);
					assertRoute(result, "testDelete", Status.OK, null, true);
				}//run
			}//Runnable() 
		);//running
	}//beforeTest()

	@Override
	protected int getProfile1DisabledReturnCode() {
		return 503;
	}

	@Override
	protected int getProfile1SwitchReturnCode() {
		return 400;
	}
	
}//class
