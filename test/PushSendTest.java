import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import com.baasbox.service.logging.BaasBoxLogger;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.security.SessionKeys;
import com.baasbox.service.push.providers.APNServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;

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
					BaasBoxLogger.debug("URL to check login_info in system: " + url);
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
					
                    // Create user
					request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUserNotAccess+"\"", true);

					url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
					BaasBoxLogger.debug("URL to check signupdate in system: " + url);
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
					
					//test verbose for admins
					node = updatePayloadFieldValue("/pushPayloadWithoutProfileSpecifiedWithUser.json", "users", new String[]{sFakeUser});
					request = new FakeRequest("POST", "/push/message?verbose=true");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
					request = request.withJsonBody(node,"POST");
					result = routeAndCall(request);
					assertRoute(result,"testSendPushWithNewApi - ok", 200, "Profiles computed:", true);
					
					//verbose is not active for reg users
					node = updatePayloadFieldValue("/pushPayloadWithoutProfileSpecifiedWithUser.json", "users", new String[]{sFakeUser});
					request = new FakeRequest("POST", "/push/message?verbose=true");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUserNotAccess, sPwd));
					request = request.withJsonBody(node,"POST");
					result = routeAndCall(request);
					assertRoute(result,"testSendPushWithNewApi - ok", 200, "has been sent", true);
					
					//send a message to more than a device (up to 10) at the same time
					//populate login_info with iOS Token
					for (int i=0;i<=9;i++){
						request = new FakeRequest("PUT","/push/enable/ios/"+ UUID.randomUUID());
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
						result = routeAndCall(request);
						assertRoute(result,"populate login_info",200,null,true);
					}
					node = updatePayloadFieldValue("/pushPayloadWithoutProfileSpecifiedWithUser.json", "users", new String[]{sFakeUser});
					request = new FakeRequest("POST", "/push/message?verbose=true");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUserNotAccess, sPwd));
					request = request.withJsonBody(node,"POST");
					result = routeAndCall(request);
					assertRoute(result,"testSendPushWithNewApi - ok", 200, "has been sent", true);
					
					
				}
			}
		);
	}
	
	//Test for #592 NPE sending Push if content-available key is missing
	
	@Test
	public void TestApnServer(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUserNotAccess = "testpushuser_" + UUID.randomUUID();
					// Prepare test user
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUserNotAccess);
			        String sPwd = getPayloadFieldValue("/adminUserCreatePayload.json", "password");
					
					// Create user
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUserNotAccess+"\"", true);
					
					try{
						DbHelper.open("1234567890", sFakeUserNotAccess, sPwd);
						ObjectMapper om = BBJson.mapper();
						JsonNode payload = om.readTree("{"+
								"\"custom\" :     {"+
								"		\"QBKey\" : \"12a535fb-5732-44e0-8a97-0a4688ba75ba\","+
								"		\"QBName\" : \"QBPushNotificationName\""+
								"},"+
								"\"message\" : \"This is a message.\""+
								"}");
						boolean validate = APNServer.validatePushPayload(payload);
						Assert.assertTrue("payload is not valid!", validate);
					} catch (InvalidAppCodeException | BaasBoxPushException | IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					}finally{
						if (DbHelper.getConnection()!=null && !DbHelper.getConnection().isClosed()) DbHelper.close(DbHelper.getConnection());
					}
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
