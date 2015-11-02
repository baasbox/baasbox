import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;

import core.TestConfig;



public class CacheScriptTest {

	private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();
	private final static String BODY_REQ = "{\"cacheType\":\"%s\",\"key\":\"%s\",\"value\":%s}";
	@BeforeClass
	public static void installScript(){
		running(fakeApplication(),()->{
			try {
				DbHelper.open("1234567890", "admin", "admin");
				ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_cache.js");
			}catch (Throwable e){
				fail(ExceptionUtils.getStackTrace(e));
			} finally {
				DbHelper.close(DbHelper.getConnection());
			}
		});
	}


	private void writeValueInCache(String username,String cacheType,String key,Object value) {
		FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL);
		req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username,"passw1"));
		req = req.withHeader(CONTENT_TYPE,"application/json");
		try{
			System.out.println(String.format(BODY_REQ, cacheType,key,BBJson.mapper().writeValueAsString(value)));
			req=req.withJsonBody(BBJson.mapper().readTree(String.format(BODY_REQ, cacheType,key,BBJson.mapper().writeValueAsString(value))));
		}catch(Exception e){
			fail("Unable to write body to JSON");
		}
		Result res = route(req);
		Assert.assertEquals("SET VALUE IN CACHE",200,Helpers.status(res));

	}
	private  String getValueFromCache(String username,String cacheType,String key) {
		FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL+"?cacheType="+cacheType+"&key="+key);
		req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username,"passw1"));
		req = req.withHeader(CONTENT_TYPE,"application/json");
		Result res = route(req);
		Assert.assertEquals("GET VALUE FROM CACHE",200,Helpers.status(res));
		String content= contentAsString(res);
		return content;

	}
	@Test
	public void testGlobalCacheFromTwoDifferentUsers() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			String username2 = new AdminUserFunctionalTest().createNewUser("user2");
			writeValueInCache(username, "global", "key1", "hello world");
			String getResult = getValueFromCache(username2,"global", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals("hello world",jn.get("data").get("value").asText());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}

		});
	}

	@Test
	public void testLocalCacheFromTwoDifferentUsers() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			String username2 = new AdminUserFunctionalTest().createNewUser("user2");
			writeValueInCache(username, "local", "key1", "hello world");
			String getResult = getValueFromCache(username2,"global", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals(404,jn.get("data").get("status").asInt());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}

	});
}

}
