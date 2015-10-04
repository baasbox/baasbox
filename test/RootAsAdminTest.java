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


public class RootAsAdminTest extends AbstractTest {



		@Test
		public void testRootIsAnAdmin(){
			continueOnFail(true);
			//root as admin is enabled
			HashMap overrideConf1=new HashMap();
			overrideConf1.put("baasbox.list.response.chunked", false);
			overrideConf1.put("baasbox.root.password","root");
			overrideConf1.put("baasbox.root.admin", true);
			
			FakeApplication fakeApp1 = play.test.Helpers.fakeApplication(overrideConf1);
			Helpers.start(fakeApp1);
				FakeRequest request1 = new FakeRequest("GET", "/admin/collection");
				request1 = request1.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
				request1 = request1.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
				Result result1=Helpers.route(request1);
				assertRoute(result1, "root enabled, admin call", 200, "{\"result\":\"ok\",\"data\":[", true);
				
				request1 = new FakeRequest("GET", "/file/details");
				request1 = request1.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
				request1 = request1.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
				result1=Helpers.route(request1);
				assertRoute(result1, "root enabled, user call", 200, "{\"result\":\"ok\",\"data\":[", true);
			Helpers.stop(fakeApp1);	
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
