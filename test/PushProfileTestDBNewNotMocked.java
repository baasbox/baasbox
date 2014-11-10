import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.After;
import org.junit.Before;

import com.baasbox.BBConfiguration;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.TestConfig;


public class PushProfileTestDBNewNotMocked extends PushProfileAbstractTestNotMocked {

	private Boolean oldMockValue;
	public PushProfileTestDBNewNotMocked() {}

	@Before
	public void beforeTest(){
		//import db
		running
		(
			getFakeApplicationWithDefaultConf(), 
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
					oldMockValue=BBConfiguration.getPushMock();
					BBConfiguration._overrideConfigurationPushMock(false);
				}//run
			}//Runnable() 
		);//running
	}//beforeTest()

	@After
	public void afterTest(){
		//import db
		running	(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>()  {
				public void invoke(TestBrowser browser) {
					BBConfiguration._overrideConfigurationPushMock(oldMockValue);
				}
			}
			);
	}
	
	@Override
	protected int getProfile1DisabledReturnCode() {
		return 503;
	}

	@Override
	protected int getProfile1SwitchReturnCode() {
		return 200;
	}
	
}//class
