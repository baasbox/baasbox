import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.PUT;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.security.SessionKeys;
import com.baasbox.service.logging.BaasBoxLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.AbstractTest;
import core.TestConfig;


public class SocialTest extends AbstractTest {

	
		@Test
		public void testSocial(){
			running	(
					getTestServer(), 
					HTMLUNIT, 
					new Callback<TestBrowser>() 
			        {
						public void invoke(TestBrowser browser) 
						{
						//after a Signup using a social network, the user data are returned, issue #504
							// Prepare test user
							String o_token=UUID.randomUUID().toString();
							JsonNode node = updatePayloadFieldValue("/socialSignup.json", "oauth_token", o_token);
	
							// Create user
							FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, getMethod());
							Result result = routeAndCall(request);						
							assertRoute(result, "testSocial - check user no rid", 200, "\"user\":{\"name\":", true);
							
							String body = play.test.Helpers.contentAsString(result);
							JsonNode jsonRes = Json.parse(body);
							String sessionToken = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
							String username = jsonRes.get("data").get("user").get("name").textValue();
						
						//there is the signupdate in system object?
							String url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
							BaasBoxLogger.debug("URL to check signupdate in system: " + url);
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - signupdate in system", 200, "\"signUpDate\":\"", true);
						
						//users cannot access to system properties belonging to other users
							String sFakeUser = "testsocialnormaluser_" + UUID.randomUUID();
							// Prepare test user
							node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
	
							// Create user
							request = new FakeRequest("POST", "/user");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, "POST");
							result = routeAndCall(request);
							assertRoute(result, "routeCreateUser check username", Status.CREATED, "name\":\""+sFakeUser+"\"", true);

							url="/users?fields=system%20as%20s&where=user.name%3D%22"+username+"%22";
							BaasBoxLogger.debug("URL to check signupdate in system: " + url);
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUser, "passw1"));
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - no access to system attribute by other users", 200, "s\":null", true);
						
						//users have social data property	
							url="/users?&where=user.name%3D%22"+username+"%22";
							request = new FakeRequest("GET",url);
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sFakeUser, "passw1"));
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - visibleByRegisteredUsers._social must be present", 200, "\"visibleByRegisteredUsers\":{\"_social\":{\"facebook\":{\"id\"", true);
							
						//the social user logins again using the same social token and .... it has to be the same user
						//this test tests the login using social network
							node = updatePayloadFieldValue("/socialSignup.json", "oauth_token", o_token);
							request = new FakeRequest(getMethod(), getRouteAddress());
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, getMethod());
							result = routeAndCall(request);						
							assertRoute(result, "testSocial - check user no rid", 200, "\"user\":{\"name\":", true);
							
							body = play.test.Helpers.contentAsString(result);
							JsonNode jsonResCheck = Json.parse(body);
							String sessionTokenCheck = jsonResCheck.get("data").get(SessionKeys.TOKEN.toString()).textValue();
							String usernameCheck = jsonResCheck.get("data").get("user").get("name").textValue();
							Assert.assertTrue("Usernames must be equal. At signup: " + username + ", at login: " + usernameCheck, username.equals(usernameCheck));
							Assert.assertTrue("Session tokens must be different. At signup: " + sessionToken + ", at login: " + sessionTokenCheck, !sessionToken.equals(sessionTokenCheck));
							
							
							ObjectNode objectNodeRes = (ObjectNode)jsonRes;
							ObjectNode objectNodeResCheck = (ObjectNode)jsonResCheck;
							((ObjectNode) objectNodeRes.get("data")).remove("X-BB-SESSION");
							((ObjectNode) objectNodeResCheck.get("data")).remove("X-BB-SESSION");							
							Assert.assertTrue("Social login must return same data (but X-BB-SESSION) of social signup:\nobjectNodeRes:\n" + objectNodeRes.toString()+"\n\nobjectNodeResCheck:\n" + objectNodeResCheck.toString(),objectNodeRes.equals(objectNodeResCheck));
						
							//users cannot update their _social fields
							request = new FakeRequest("PUT", "/me");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
							request = request.withJsonBody(getPayload("/adminUserUpdateNoRolePayload.json"), PUT);
							result = routeAndCall(request);
							assertRoute(result, "testSocial - cannot update their _social fields", 200, "\"_social\":{\"facebook\":{\"id\":\"mockid", true);
							
						
					}
				}
			);		
		}

		@Test
		public void testSocialChangeUsername(){
			running	(
					getTestServer(), 
					HTMLUNIT, 
					new Callback<TestBrowser>() 
			        {
						public void invoke(TestBrowser browser) throws JsonProcessingException, IOException 
						{
							// Prepare test user
							String o_token=UUID.randomUUID().toString();
							JsonNode node = updatePayloadFieldValue("/socialSignup.json", "oauth_token", o_token);
	
							// Create user
							FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withJsonBody(node, getMethod());
							Result result = routeAndCall(request);						
							assertRoute(result, "testSocialChangeUsername - create user", 200, "\"user\":{\"name\":", true);
							
							String body = play.test.Helpers.contentAsString(result);
							JsonNode jsonRes = Json.parse(body);
							String sessionToken = jsonRes.get("data").get(SessionKeys.TOKEN.toString()).textValue();
							String username = jsonRes.get("data").get("user").get("name").textValue();
							
							//now let's change the username (issue 426)
							
							request = new FakeRequest("PUT", "/me/username");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);
							ObjectMapper om = new ObjectMapper();
							String newUsername="socialUserRenamed_" + username;
							JsonNode nodeChangeUsername = om.readTree("{\"username\":\"" + newUsername + "\"}");
							request = request.withJsonBody(nodeChangeUsername, PUT);
							result = routeAndCall(request);
							assertRoute(result, "testSocialChangeUsername - change username", 200, "", false);
							
							//issue 724 - change username feature must change the friendship role too
							try {
								DbHelper.open("1234567890", "admin", "admin");
								Object checkChangeRole=DbHelper.genericSQLStatementExecute("select from ORole where name=?", 
										new String []{RoleDao.getFriendRoleName(newUsername)});
								Assert.assertTrue("ChangeUsername: The role has not be changed",((List)checkChangeRole).size()==1);
							} catch (Exception e) {
								Assert.fail(ExceptionUtils.getFullStackTrace(e));
							} finally{
								DbHelper.close(DbHelper.getConnection());
							}
							
							//perform a social login again
							FakeRequest request2 = new FakeRequest(getMethod(), getRouteAddress());
							request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request2 = request2.withJsonBody(node, getMethod());
							Result result2 = routeAndCall(request2);						
							assertRoute(result2, "testSocialChangeUsername - login again", 200, "\"user\":{\"name\":", true);
							String body2 = play.test.Helpers.contentAsString(result2);
							JsonNode jsonRes2 = Json.parse(body2);
							String sessionToken2 = jsonRes2.get("data").get(SessionKeys.TOKEN.toString()).textValue();
							String username2 = jsonRes2.get("data").get("user").get("name").textValue();
							
							Assert.assertTrue("Username is not valid. It has not been changed. Original: " + username + ", 2nd call: " + username2, !username2.equals(username));
							Assert.assertTrue("Username is not valid. It has not been changed. Received: " + username2 + ", I want: " + newUsername, username2.equals(newUsername));
						
							//old token is not valid
							request = new FakeRequest("POST", "/logout");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken);	
							result = routeAndCall(request);
							assertRoute(result, "testSocialChangeUsername - logout", 401, username, true);
							
							//now let's try the new token. Performing a logout....
							request = new FakeRequest("POST", "/logout");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_TOKEN, sessionToken2);	
							result = routeAndCall(request);
							assertRoute(result, "testSocialChangeUsername - logout", 200, "", false);
							
						}
			        });
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
