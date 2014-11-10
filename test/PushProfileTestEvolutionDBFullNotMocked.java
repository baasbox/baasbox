import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;

import com.baasbox.BBConfiguration;

import play.libs.F.Callback;
import play.test.TestBrowser;
import core.TestConfig;


public class PushProfileTestEvolutionDBFullNotMocked extends PushProfileAbstractTestNotMocked {

	private Boolean oldMockValue;
	public PushProfileTestEvolutionDBFullNotMocked() {}

	@Before
	public void beforeTest(){
		//import db
		running	(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>()  {
				public void invoke(TestBrowser browser) {
					//load a backup we did using v.0.8.3
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/BB_export_083_push_test.zip", "application/zip");
					int status = httpRequest("http://localhost:3333/admin/db/import", POST,new HashMap<String,String>());
					assertTrue(status==200);
					oldMockValue=BBConfiguration.getPushMock();
					BBConfiguration._overrideConfigurationPushMock(false);
				}//invoke
			}//Callback<TestBrowser>() 
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
		return 200;
	}

	@Override
	protected int getProfile1SwitchReturnCode() {
		return 200;
	}
	
}//class
