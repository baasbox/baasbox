import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.db.DbHelper;
import com.baasbox.security.SessionKeys;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractTest;
import core.TestConfig;


public class PushTest_Issue886 extends AbstractTest{
	
	private final static String TEST_CALL="test.send_push_"+ScriptTestHelpers.randomScriptName();
	
	@BeforeClass
	public static void installScript(){
		running(getFakeApplicationWithRealPushProvider(),()->{
			try {
				DbHelper.open("1234567890", "admin", "admin");
				ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_push_886.js");
			}catch (Throwable e){
				fail(ExceptionUtils.getStackTrace(e));
			} finally {
				DbHelper.close(DbHelper.getConnection());
			}
		});
	}
	//@Test
	public void PushSendWithPlugin(){
		running
		(
				getFakeApplicationWithRealPushProvider(), 
			new Runnable() 
			{
				public void run() 
				{
					String sFakeUser1 = "testpushuser_" + UUID.randomUUID();
					JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser1);

					// Create user
					FakeRequest request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					Result result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUser1+"\"", true);
					
					String body = play.test.Helpers.contentAsString(result);
					JsonNode jsonRes = Json.parse(body);
					String username1 = jsonRes.get("data").get("user").get("name").textValue();
					
					//populate login_info with iOS Token
					String sessionToken1 = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					
					String sFakeUser2 = "testpushuser_" + UUID.randomUUID();
					node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser2);

					// Create user
					request = new FakeRequest("POST", "/user");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withJsonBody(node, "POST");
					result = routeAndCall(request);
					assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUser2+"\"", true);
				
					body = play.test.Helpers.contentAsString(result);
					jsonRes = Json.parse(body);
					String username2 = jsonRes.get("data").get("user").get("name").textValue();
					
					//populate login_info with iOS Token
					String sessionToken2 = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					
					
					request = new FakeRequest("PUT","/push/enable/ios/"+ UUID.randomUUID());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken1);
					result = routeAndCall(request);
					assertRoute(result,"populate login_info",200,null,true);
					
					//populate login_info with Android Token
					sessionToken1 = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
					for(int i=0;i<20;i++){
						request = new FakeRequest("POST","/login");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(username1, "passw1"));
						request = request.withHeader("Content-Type","application/json");
						try{
							request = request.withJsonBody(Json.parse("{\"username\":\""+username1+"\",\"password\":\"passw1\",\"appcode\":\"1234567890\"}"));
						}catch(Exception e){
							fail();
						}
						result = routeAndCall(request);
						
						body = play.test.Helpers.contentAsString(result);
						jsonRes = Json.parse(body);
						
						sessionToken1 = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
						
						boolean ios = new Random().nextInt(2)% 2 ==0;
						request = new FakeRequest("PUT","/push/enable/"+ (ios?"ios":"android") +"/"+ UUID.randomUUID());
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken1);
						result = routeAndCall(request);
						assertRoute(result,"populate login_info",200,null,true);
						
					}
					
					
					FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL);
					req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
					req = req.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					req = req.withHeader(CONTENT_TYPE,"application/json");
					try {
						req = req.withJsonBody(BBJson.mapper().readTree("{\"username\":\""+username1+"\"}"));
					} catch (IOException e) {
						fail();
					}
					Result res = route(req);
					Assert.assertEquals("GET VALUE FROM LINK PLUGIN",200,Helpers.status(res));
					String content= contentAsString(res);
					System.out.println(content);
					
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
		return;
	}
}
