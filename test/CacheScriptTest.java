import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import core.TestConfig;




public class CacheScriptTest {

	private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();
	private final static String BODY_REQ = "{\"cacheScope\":\"%s\",\"key\":\"%s\",\"value\":%s}";
	private final static String BODY_WITH_TTL = "{\"cacheScope\":\"%s\",\"key\":\"%s\",\"value\":%s,\"ttl\":%d}";
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


	private void writeValueInCache(String username,String cacheScope,String key,Object value,Integer ttl) {
		FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL);
		req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username,"passw1"));
		req = req.withHeader(CONTENT_TYPE,"application/json");
		try{
			if(ttl==0){
				req=req.withJsonBody(BBJson.mapper().readTree(String.format(BODY_REQ, cacheScope,key,BBJson.mapper().writeValueAsString(value))));
			}else{
				req=req.withJsonBody(BBJson.mapper().readTree(String.format(BODY_WITH_TTL, cacheScope,key,BBJson.mapper().writeValueAsString(value),ttl)));
			}
		}catch(Exception e){
			fail("Unable to write body to JSON");
		}
		Result res = route(req);
		Assert.assertEquals("SET VALUE IN CACHE",200,Helpers.status(res));

	}
	private  String getValueFromCache(String username,String cacheType,String key) {
		FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL+"?cacheScope="+cacheType+"&key="+key);
		req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username,"passw1"));
		req = req.withHeader(CONTENT_TYPE,"application/json");
		Result res = route(req);
		Assert.assertEquals("GET VALUE FROM CACHE",200,Helpers.status(res));
		String content= contentAsString(res);
		return content;

	}
	
	private  void removeValueFromCache(String username,String cacheType,String key) {
		FakeRequest req = new FakeRequest("DELETE","/plugin/"+TEST_CALL+"?cacheScope="+cacheType+"&key="+key);
		req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
		req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(username,"passw1"));
		Result res = route(req);
		Assert.assertEquals("REMOVE VALUE FROM CACHE",200,Helpers.status(res));
		

	}
	@Test
	public void testGlobalCacheFromTwoDifferentUsers() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			String username2 = new AdminUserFunctionalTest().createNewUser("user2");
			writeValueInCache(username, "app", "key1", "hello world",0);
			String getResult = getValueFromCache(username2,"app", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals("hello world",jn.get("data").asText());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			writeValueInCache(username2, "app", "key1", "hello world 2",0);
			String getResult2 = getValueFromCache(username,"app", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult2);
				Assert.assertEquals("hello world 2",jn.get("data").asText());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}

		});
	}
	
	@Test
	public void testSetAndRemove() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			writeValueInCache(username, "user", "key1", "hello world",0);
			String getResult = getValueFromCache(username,"user", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals("hello world",jn.get("data").asText());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			removeValueFromCache(username,"user","key1");
			getResult = getValueFromCache(username,"user", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals(404,jn.get("data").get("status").asInt());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			
		});
	}
	
	@Test
	public void testSetComplexObject() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			
			writeValueInCache(username, "user", "key_complex", complexObject(),0);
			String getResult = getValueFromCache(username,"user", "key_complex");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				String data = jn.get("data").asText();
				Map<String,Object> result = BBJson.mapper().readValue(data, new TypeReference<Map<String,Object>>(){});
				Assert.assertNotNull(result);
				Assert.assertNotNull(result.get("doubleValue"));
				Assert.assertNotNull(result.get("floatValue"));
				Assert.assertNotNull(result.get("stringValue"));
				Assert.assertNotNull(result.get("booleanValue"));
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			removeValueFromCache(username,"user","key1");
			getResult = getValueFromCache(username,"user", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals(404,jn.get("data").get("status").asInt());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			
		});
	}

	
	private String complexObject() {
		Random r = new Random();
		Map<String,Object> obj = new HashMap<String,Object>();
		obj.put("doubleValue", r.nextDouble());
		obj.put("floatValue", r.nextFloat());
		obj.put("stringValue", "hello world"+r.nextInt(100));
		obj.put("booleanValue",r.nextInt(100)%2==0?true:false);
		try {
			return BBJson.mapper().writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			fail("Unable to serialize map");
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testSetAndReadWithTTL() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			writeValueInCache(username, "user", "key1", "hello world",2);
			String getResult = getValueFromCache(username,"user", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals("hello world",jn.get("data").asText());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}
			try {
				Thread.sleep(3000);
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals(404,jn.get("data").get("status").asInt());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
	}

	@Test
	public void testLocalCacheFromTwoDifferentUsers() {
		running(fakeApplication(),()->{
			String username = new AdminUserFunctionalTest().createNewUser("user1");
			String username2 = new AdminUserFunctionalTest().createNewUser("user2");
			writeValueInCache(username, "user", "key1", "hello world",0);
			String getResult = getValueFromCache(username2,"user", "key1");
			try{
				JsonNode jn = BBJson.mapper().readTree(getResult);
				Assert.assertEquals(404,jn.get("data").get("status").asInt());
			}catch(IOException ioe){
				fail(ioe.getMessage());
			}

	});
}

}
