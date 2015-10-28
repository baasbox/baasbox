import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.security.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractTest;
import core.TestConfig;


public class RootIsNotAnAdminTest extends AbstractTest {
/*	
	@Test
	public void testRootIsNotAdmin(){
		continueOnFail(true);
		
		//root as admin is disabled
		HashMap overrideConf2=new HashMap();
		overrideConf2.put("baasbox.list.response.chunked", false);
		overrideConf2.put("baasbox.root.password","root");
		overrideConf2.put("baasbox.root.admin", false);
		
		FakeApplication fakeApp2 = play.test.Helpers.fakeApplication(overrideConf2);
		Helpers.start(fakeApp2);
			FakeRequest request2 = new FakeRequest("GET", "/admin/collection");
			request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
			request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
			Result result2=Helpers.route(request2);
			assertRoute(result2, "root disabled, admin call", 401, "User root is not authorized to access", true);
			
			request2 = new FakeRequest("GET", "/file/details");
			request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
			request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
			result2=Helpers.route(request2);
			assertRoute(result2, "root disabled, user call", 401, "User root is not authorized to access", true);
		Helpers.stop(fakeApp2);
	}
		
*/	
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
