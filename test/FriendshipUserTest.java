import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;


public class FriendshipUserTest extends AbstractTest{

   public static final String USER1_TEST = "user1";  
   public static final String USER2_TEST = "user2";
   public static final String USER3_TEST = "user3";
   
   public static final String USER4_TEST = "user4";
   public static final String USER5_TEST = "user4";
	
 
   
	@Override
	public String getRouteAddress()
	{
		return "/follow";
	}
	
	@Override 
	public String getMethod()
	{
		return POST;
	}
	
	@Override
	protected void assertContent(String s)
	{
	}
	
	public String routeCreateNewUser(String username)
	{
		String sFakeUser = username + "-" + UUID.randomUUID();
		// Prepare test user
		JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

		// Create user
		FakeRequest request = new FakeRequest(POST, "/user");
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(node, POST);
		Result result = routeAndCall(request);
		assertRoute(result, "Create user.", Status.CREATED, null, false);

		return sFakeUser;
	}
	
	public void routeCreateNewFollowRelationship(String follower,String toFollow)
	{
		
		FakeRequest request = new FakeRequest(POST, getRouteAddress()+"/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
		
		Result result = routeAndCall(request);
		assertRoute(result, "Create friendship.", Status.CREATED, "+39334060606",true);
		assertRoute(result, "Create friendship. 2", Status.CREATED, "\"visibleByTheUser\":null",true);
	}
	
	public void routeCreateNewUnexistentFollowRelationship(String follower,String toFollow)
	{
		
		FakeRequest request = new FakeRequest(POST, getRouteAddress()+"/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
		
		Result result = routeAndCall(request);
		assertRoute(result, "Create friendship.", Status.NOT_FOUND, "User "+toFollow+" does not exists.", true);

		
	}
	
	public void routeDeleteFollowRelationship(String follower,String toUnfollow)
	{
		
		FakeRequest request = new FakeRequest(DELETE, getRouteAddress()+"/"+toUnfollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
		
		Result result = routeAndCall(request);
		assertRoute(result, "Delete friendship.", Status.OK, null, false);
		//now if I ask for the ex-friend profile, I couldn't see its FriendAttributes fields
		
		request = new FakeRequest(GET, "/user/"+toUnfollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
		result = routeAndCall(request);
		assertRoute(result, "Check no friendship.", Status.OK, null, false);
		String resultString = contentAsString(result);
		if (resultString.contains("+39334060606")) assertFail("FriendshipUserTest (routeDeleteFollowRelationship) failed! The ex-friend still see private fields!");
		
	}
	
	public void routeDeleteUnexistentFollowRelationship(String follower,String toUnfollow)
	{
		
		FakeRequest request = new FakeRequest(DELETE, getRouteAddress()+"/"+toUnfollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
		
		Result result = routeAndCall(request);
		assertRoute(result, "Delete friendship.", Status.NOT_FOUND, null, false);

		
	}
	public void routeCreateCollectionForTest()
	{
		
		FakeRequest request = new FakeRequest(POST, "/admin/collection/testFriendship");
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		
		Result result = routeAndCall(request);
		
	}
	
	public String routeCreateDocument(String owner)
	{
		
		FakeRequest request = new FakeRequest(POST, "/document/testFriendship");
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(owner, "passw1"));
		request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
		Result result = routeAndCall(request);
		assertRoute(result, "Create document.", Status.OK, null, false);
		return contentAsString(result);
		
	}
	
	@Test
	public void createUsersAndFollow(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
					routeCreateCollectionForTest();
					
					String firstUser  = routeCreateNewUser(USER1_TEST);
					String secondUser = routeCreateNewUser(USER2_TEST);
					assertNotNull(firstUser,"First User creation");
					assertNotNull(secondUser,"Second User creation");
					routeCreateNewFollowRelationship(firstUser, secondUser);
					String content = routeCreateDocument(secondUser);
					JSONObject result = (JSONObject)toJSON(content);
					try {
						String uuid = result.getJSONObject("data").getString("id");
						assertNotNull(uuid);
						FakeRequest fk = new FakeRequest(GET, "/document/testFriendship/"+uuid);
						fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(firstUser, "passw1"));
						Result getDocumentResult = routeAndCall(fk);
						assertRoute(getDocumentResult, "Get document by friend", Status.NOT_FOUND, null, false);
						
						//the owner of the doc gives grant to its friends
						fk = new FakeRequest(PUT, "/document/testFriendship/"+uuid+"/read/role/friends_of_"+secondUser);
						fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(secondUser, "passw1"));
						Result res = routeAndCall(fk);
						assertRoute(res, "Change the document permission", Status.OK, null, false);
						
						//now the friend reload the doc and read it
						fk = new FakeRequest(GET, "/document/testFriendship/"+uuid);
						fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(firstUser, "passw1"));
						getDocumentResult = routeAndCall(fk);
						assertRoute(getDocumentResult, "Get document by friend 2", Status.OK, null, false);
						JSONObject createdDocument = (JSONObject)toJSON(contentAsString(getDocumentResult));
						String author = createdDocument.getJSONObject("data").getString("_author");
						assertEquals(secondUser,author);
						
						FakeRequest theFolloweds = new FakeRequest(GET, "/following");
						theFolloweds = theFolloweds.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						theFolloweds = theFolloweds.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(firstUser, "passw1"));
						
						Result getTheFollowedResult = routeAndCall(theFolloweds);
						JSONObject followedJson = (JSONObject)toJSON(contentAsString(getTheFollowedResult));
						String followed = followedJson.getJSONArray("data").getJSONObject(0).getJSONObject("user").getString("name");
						assertEquals(followed,secondUser);
						
						//check the followers
						FakeRequest followers = new FakeRequest(GET, "/followers");
						followers = followers.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						followers = followers.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(secondUser, "passw1"));
						Result getFollowersResult = routeAndCall(followers);
						JSONObject followersJson = (JSONObject)toJSON(contentAsString(getFollowersResult));
						String follower = followersJson.getJSONArray("data").getJSONObject(0).getJSONObject("user").getString("name");
						assertEquals(follower,firstUser);
						
						//the first user get the followers of the second one
						followers = new FakeRequest(GET, "/followers/"+secondUser);
						followers = followers.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						followers = followers.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(firstUser, "passw1"));
						getFollowersResult = routeAndCall(followers);
						followersJson = (JSONObject)toJSON(contentAsString(getFollowersResult));
						follower = followersJson.getJSONArray("data").getJSONObject(0).getJSONObject("user").getString("name");
						assertEquals("test: GET /followers/:secondUser firstUser="+firstUser+" secondUser="+secondUser,follower,firstUser);

						//the second user get the followed by the first one
						FakeRequest followeds = new FakeRequest(GET, "/following/"+firstUser);
						followeds = followeds.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						followeds = followeds.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(secondUser, "passw1"));
						Result getFollowedsResult = routeAndCall(followeds);
						JSONObject followedsJson = (JSONObject)toJSON(contentAsString(getFollowedsResult));
						followed = followedsJson.getJSONArray("data").getJSONObject(0).getJSONObject("user").getString("name");
						assertEquals("test: GET /following/:firstUser firstUser="+firstUser+" secondUser="+secondUser,followed,secondUser);
						
						routeDeleteFollowRelationship(firstUser,secondUser);
						getDocumentResult = routeAndCall(fk);
						assertRoute(getDocumentResult, "Get document by unknows", Status.NOT_FOUND, null, false);
					} catch (JSONException e) {
						fail();
					}
					
					
				}
			});
	}
	
	@Test
	public void unexistentUserFollow(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
					routeCreateCollectionForTest();
					
					String firstUser  = routeCreateNewUser(USER3_TEST);
					assertNotNull(firstUser,"First User creation");
					
					routeCreateNewUnexistentFollowRelationship(firstUser, "unexistent");
					
					
				}
			});
	}
	
	@Test
	public void unexistentFollow(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
					
					
					String firstUser  = routeCreateNewUser(USER4_TEST);
					String secondUser  = routeCreateNewUser(USER5_TEST);
					assertNotNull(firstUser,"First User creation");
					assertNotNull(secondUser,"Second User creation");
					
					routeDeleteUnexistentFollowRelationship(firstUser,secondUser);
					
					
				}
			});
	}
}
