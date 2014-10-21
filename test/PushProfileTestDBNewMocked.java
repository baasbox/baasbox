import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.Before;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.TestConfig;


public class PushProfileTestDBNewMocked extends PushProfileAbstractTestMocked {

	public PushProfileTestDBNewMocked() {}

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
		return 200;
	}
	
}//class
