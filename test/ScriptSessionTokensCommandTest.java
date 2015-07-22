import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.security.SessionKeys;
import com.baasbox.security.SessionTokenProvider;
import com.baasbox.util.BBJson;
import com.baasbox.util.BBJson.ObjectMapperExt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import core.AbstractUserTest;
import core.TestConfig;

/**
 * Created by eto on 29/09/14.
 */
public class ScriptSessionTokensCommandTest  extends AbstractUserTest{

    private static TreeSet<String>  sRandUsers;
    private static String sTestUser;
    private static final ObjectMapperExt mapper = BBJson.mapper();
    private static final String USER_PREFIX = "script-sessions-test-";
    private static String key;
    private String user1=null;
    private String user2=null;
    private String password="passw1"; //in adminUserCreatePayload.json
    private String token1=null;
    private String token2=null;
    
    
    
    //let's create a couple of users to use during the tests
    @Before
    public  void createUsers() {
    	running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					try{
				    	user1 = USER_PREFIX + UUID.randomUUID();
						// Prepare test user
						JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", user1);
				
						// Create user
						FakeRequest request = new FakeRequest("POST", "/user");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withJsonBody(node, "POST");
						Result result = routeAndCall(request);
						String resAsString = Helpers.contentAsString(result);
						ObjectNode resAsJson = (ObjectNode) mapper.readTree(resAsString);
						token1 = resAsJson.get("data").get("X-BB-SESSION").asText();
						
				    	user2 = USER_PREFIX + UUID.randomUUID();
						// Prepare test user
						node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", user2);
				
						// Create user
						request = new FakeRequest("POST", "/user");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withJsonBody(node, "POST");
						result = routeAndCall(request);
						resAsString = Helpers.contentAsString(result);
						resAsJson = (ObjectNode) mapper.readTree(resAsString);
						token2 = resAsJson.get("data").get("X-BB-SESSION").asText();
					}catch(Exception e){
						assertFail(ExceptionUtils.getFullStackTrace(e));
					}
				}
			});
    }

    @Test
    public void test(){
    	running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					try{
						List<ImmutableMap<SessionKeys, ? extends Object>> sessions = SessionTokenProvider.getSessionTokenProvider().getSessions(user1);
						Assert.assertEquals(1, sessions.size());
						Assert.assertEquals(true,sessions.get(0).get(SessionKeys.USERNAME).equals(user1));
						Assert.assertEquals("token1: " + token1 + ", received: " + sessions.get(0).get(SessionKeys.TOKEN),true,sessions.get(0).get(SessionKeys.TOKEN).equals(token1));
						  
						List<ImmutableMap<SessionKeys, ? extends Object>> sessionsUser2 = SessionTokenProvider.getSessionTokenProvider().getSessions(user2);
						Assert.assertEquals(1, sessionsUser2.size());
						Assert.assertEquals(true,sessionsUser2.get(0).get(SessionKeys.USERNAME).equals(user2));
						Assert.assertEquals(true,sessionsUser2.get(0).get(SessionKeys.TOKEN).equals(token2));
						  
						SessionTokenProvider.getSessionTokenProvider().removeSession(token1);
						sessions = SessionTokenProvider.getSessionTokenProvider().getSessions(user1);
						Assert.assertEquals(0, sessions.size());
						
						SessionTokenProvider.getSessionTokenProvider().setSession("1234567890", user1, password);
						sessions = SessionTokenProvider.getSessionTokenProvider().getSessions(user1);
						Assert.assertEquals(1, sessions.size());
						
					}catch(Exception e){
						assertFail(ExceptionUtils.getFullStackTrace(e));
					}
				}
			});
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
