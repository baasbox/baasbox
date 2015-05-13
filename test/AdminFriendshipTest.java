
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;
public class AdminFriendshipTest extends AbstractTest{


	public void createFriendship(String follower,String toFollow)
	{

		// Create user
		FakeRequest request = new FakeRequest(POST, getRouteAddress()+"/"+follower+"/to/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		assertRoute(result, "Create friendship.", Status.CREATED, null, false);
	}
	public void deleteFriendship(String follower,String toFollow)
	{

		// Create user
		FakeRequest request = new FakeRequest(DELETE, getRouteAddress()+"/"+follower+"/to/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		assertRoute(result, "Delete friendship.", Status.OK, null, false);
	}

	public void deleteUnexistentFriendship(String follower,String toFollow)
	{

		// Create user
		FakeRequest request = new FakeRequest(DELETE, getRouteAddress()+"/"+follower+"/to/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		assertRoute(result, "Delete unexistent friendship.", Status.NOT_FOUND, null, false);
	}

	@Override
	public String getRouteAddress() {
		return "/admin/follow";
	}

	@Override
	public String getMethod() {
		return GET;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub

	}
	
	private void createFriendshipWithUnexistentUser(
			String follower, String toFollow) {
		FakeRequest request = new FakeRequest(POST, getRouteAddress()+"/"+follower+"/to/"+toFollow);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		assertRoute(result, "Create friendship.", Status.NOT_FOUND, null, false);
		
	}
	@Test
	public void testCreateFollowship() throws Exception{
		running
		(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						try{
						String follower = createNewUser("user1");
						String toFollow = createNewUser("user2");
						createFriendship(follower, toFollow);
						FakeRequest fk = new FakeRequest(GET,"/admin/following/"+follower);
						fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						Result r = routeAndCall(fk);
						assertRoute(r, "Get who is following.", Status.OK, null, false);
						String result = contentAsString(r);
						JSONObject resultJson = (JSONObject)toJSON(result);
						assertNotNull(resultJson);
						assertNotNull(resultJson.getJSONArray("data"));
						JSONObject followed = resultJson.getJSONArray("data").getJSONObject(0).getJSONObject("user");
						assertEquals(followed.getString("name"),toFollow);
						deleteFriendship(follower, toFollow);
						r = routeAndCall(fk);
						assertRoute(r, "Get friendships.", Status.OK, "{\"result\":\"ok\",\"data\":[],\"http_code\":200}", false);
						}catch(Exception e){
							fail(ExceptionUtils.getMessage(e));
						}
					}
				});
	}
	
	@Test
	public void testDeleteUnexistentFollowship() throws Exception{
		running
		(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						try{
						String follower = createNewUser("user1");
						String toFollow = "user2"+UUID.randomUUID().toString();
						
						deleteUnexistentFriendship(follower, toFollow);
						
						}catch(Exception e){
							fail(ExceptionUtils.getMessage(e));
						}
					}
				});
	}
	
	@Test
	public void testCreateFollowshipWithUnexistentUser() throws Exception{
		running
		(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						try{
						String follower = createNewUser("user1");
						String toFollow = "user2"+UUID.randomUUID().toString();
						
						createFriendshipWithUnexistentUser(follower,toFollow);
						
						}catch(Exception e){
							fail(ExceptionUtils.getMessage(e));
						}
					}

					
				});
	}
}
