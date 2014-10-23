import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.ArrayList;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.baasbox.BBConfiguration;
import com.baasbox.configuration.Push;
import com.baasbox.security.SessionKeys;
import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import scala.Array;
import core.AbstractTest;
import core.TestConfig;


public class PushSendTest extends AbstractTest {

	
	@Test
	public void PushSend(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					// Create user
					String sFakeUser = "testpushuser_" + UUID.randomUUID();
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

					// Create user
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUser+"\"", true);
				
					String body = play.test.Helpers.contentAsString(result);
					JsonNode jsonRes = Json.parse(body);
					String username = jsonRes.get("data").get("user").get("name").textValue();
					
					//populate login_info with iOS Token
					String sessionToken = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					request = new FakeRequest("PUT","/push/enable/ios/"+ UUID.randomUUID());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
					result = routeAndCall(request);
					assertRoute(result,"populate login_info",200,null,true);
					
					//populate login_info with Android Token
					sessionToken = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					request = new FakeRequest("PUT","/push/enable/android/"+ UUID.randomUUID());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
					result = routeAndCall(request);
					assertRoute(result,"populate login_info",200,null,true);
					
//					there is the login_info in system object?
					String url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
					Logger.debug("URL to check login_info in system: " + url);
					request = new FakeRequest("GET",url);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
					result = routeAndCall(request);						
					assertRoute(result, "testPush - login_info in system", 200, "\"s\":{\"login_info\"", true);
					
					//users cannot access to system properties belonging to other users
	
					String sFakeUserNotAccess = "testpushuser_" + UUID.randomUUID();
					// Prepare test user
					node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUserNotAccess);
                    String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					String sAuthEnc = TestConfig.encodeAuth(sFakeUser, sPwd);
					
					// Create user
					request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUserNotAccess+"\"", true);

					url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
					Logger.debug("URL to check signupdate in system: " + url);
					request = new FakeRequest("GET",url);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUserNotAccess, sPwd));
					result = routeAndCall(request);		
					assertRoute(result, "testPush - no access to system attribute by other users", 200, "s\":null", true);
				    
				    //send push notifications with old API
				    request = new FakeRequest("POST","/push/message/"+sFakeUser);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUserNotAccess, sPwd));
					request = request.withJsonBody(getPayload("/pushPayloadWithoutProfileSpecified.json"), "POST");
					result = routeAndCall(request);
					assertRoute(result,"testSendPushWithOldApi - ok", 200, null, true);
					
				    //send push notifications with new API			
					node = updatePayloadFieldValue("/pushPayloadWithoutProfileSpecifiedWithUser.json", "users", new String[]{sFakeUser});
					request = new FakeRequest("POST", "/push/message");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUserNotAccess, sPwd));
					request = request.withJsonBody(node,"POST");
					result = routeAndCall(request);
					assertRoute(result,"testSendPushWithNewApi - ok", 200, null, true);
					
				}
			}
		);
	}
	
	
	public void PushSendTest() {
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
				}
			}
		);
		
	}
	
	
	@Override
	public String getRouteAddress() {
		return "/push/message";
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return "POST";
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}

}
