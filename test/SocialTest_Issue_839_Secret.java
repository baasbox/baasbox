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
import core.GlobalTest;
import core.TestConfig;


public class SocialTest_Issue_839_Secret extends AbstractTest {

	
		@Test
		public void test(){

				HashMap overrideSecret=new HashMap();
				overrideSecret.put("application.secret", "1234567890123456");  //<<--- set a secret
				overrideSecret.put("baasbox.social.mock", true);
				overrideSecret.put("baasbox.list.response.chunked", false);
				
				
				
				//signup
				FakeApplication fakeApp1 = getCustomFakeApplication(overrideSecret);
				Helpers.start(fakeApp1);
					HashSet <String> validSet=new HashSet<String>();
					String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();	
					String o_token=UUID.randomUUID().toString();
					JsonNode node = updatePayloadFieldValue("/socialSignup.json", "oauth_token", o_token);
					FakeRequest request1 = new FakeRequest(getMethod(), getRouteAddress());
					request1 = request1.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request1 = request1.withJsonBody(node, getMethod());
					Result result1=Helpers.route(request1);		//<<---- at first login a new fake user is created, with a fake password
					assertRoute(result1, "login 1st time user", 200, "\"user\":{\"name\":", true);
				Helpers.stop(fakeApp1);
				
				//login with different application.secret 
				overrideSecret.put("application.secret", "abcdefghilmnopqrstuvz");   //<<-- a different secret is set
				FakeApplication fakeApp2 =  getCustomFakeApplication(overrideSecret);
				Helpers.start(fakeApp2);
					FakeRequest request2 = new FakeRequest(getMethod(), getRouteAddress());
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withJsonBody(node, getMethod());
					Result result2=Helpers.route(request2);
					assertRoute(result2, "login 2nd time", 200, "\"user\":{\"name\":", true);
					
					//retrieve the token
					String body2 = play.test.Helpers.contentAsString(result2);
					JsonNode jsonRes2 = Json.parse(body2);
					String sessionToken2 = jsonRes2.get("data").get(SessionKeys.TOKEN.toString()).textValue();  // <<-- at 2nd login the password is computed but it is different from the previous one
					
					//GET from the collection
					FakeRequest request3 = new FakeRequest("GET", "/document/" + sFakeCollection);
					request3 = request3.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request3 = request3.withHeader(TestConfig.KEY_TOKEN, sessionToken2);
					Result result3=Helpers.route(request3);
					assertRoute(result3, "GET document", 200, "{\"result\":\"ok\",\"data\":[],\"http_code\":200}", true);  //<<--- if the test fails, the token is not valid and a 401 http code is returned
				Helpers.stop(fakeApp2);
				
				//login again with the older secret
				overrideSecret.put("application.secret", "1234567890123456");   //<<-- a different secret is set
				FakeApplication fakeApp3 =  getCustomFakeApplication(overrideSecret);
				Helpers.start(fakeApp3);
					FakeRequest request4 = new FakeRequest(getMethod(), getRouteAddress());
					request4 = request4.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request4 = request4.withJsonBody(node, getMethod());
					Result result4=Helpers.route(request4);
					assertRoute(result4, "login3rd time", 200, "\"user\":{\"name\":", true);
					
					//retrieve the token
					String body3 = play.test.Helpers.contentAsString(result4);
					JsonNode jsonRes3 = Json.parse(body3);
					String sessionToken3 = jsonRes3.get("data").get(SessionKeys.TOKEN.toString()).textValue();  // <<-- at 2nd login the password is computed but it is different from the previous one
					
					//GET from the collection
					FakeRequest request5 = new FakeRequest("GET", "/document/" + sFakeCollection);
					request5 = request5.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request5 = request5.withHeader(TestConfig.KEY_TOKEN, sessionToken3);
					Result result5=Helpers.route(request5);
					assertRoute(result5, "GET document 2", 200, "{\"result\":\"ok\",\"data\":[],\"http_code\":200}", true);  //<<--- if the test fails, the token is not valid and a 401 http code is returned
				Helpers.stop(fakeApp3);
				
		}
	
		@Override
		public String getRouteAddress() {
			return "/social/facebook";
		}

		@Override
		public String getMethod() {
			return "POST";
		}

		@Override
		protected void assertContent(String s) {
			// TODO Auto-generated method stub
			
		}
		
		
		
		
		
	


}
