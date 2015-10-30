import static org.junit.Assert.fail;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import static org.junit.Assert.assertTrue;

import com.baasbox.BBConfiguration;
import com.baasbox.dao.RoleDao;
import com.baasbox.service.user.FriendShipService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractRouteHeaderTest;
import core.TestConfig;

public class AdminUserDeletionTest extends AbstractRouteHeaderTest {

  @Override
  public String getRouteAddress() {
    return "/admin/user";
  }

  @Override
  public String getMethod() {
    return "GET";
  }

  @Override
  protected void assertContent(String s) {
    // TODO Auto-generated method stub

  }

  final ObjectMapper _om = new ObjectMapper();

  @Test
  public void testUserCreationWithDocumentAndDrop() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {

          String newUser = new AdminUserFunctionalTest().routeCreateNewUser();

          // Let's get the user and the role to check that have been created

          testUserExists(newUser, true);
          testRoleExists(RoleDao.getFriendRoleName(newUser), true);

          String collectionName = "coll"+UUID.randomUUID().toString();
          new AdminCollectionFunctionalTest().routeCreateCollection(collectionName);
          
          FakeRequest fq = new FakeRequest("POST","/document/"+collectionName);
          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
          try {
            fq = fq.withJsonBody(_om.readTree("{\"hello\":\"world\"}"));
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }
          Result result = routeAndCall(fq);
          assertRoute(result, "testCreateDocument", Status.OK, null, true);
          
          String documentId = null;
          try {
            String content = contentAsString(result);
            documentId = _om.readTree(content).get("data").get("id").asText();
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }
          deleteUser(newUser);

          testUserExists(newUser, false);
          testRoleExists(RoleDao.getFriendRoleName(newUser), false);
          testDocumentExists(collectionName, documentId, false);
        }

      });
  }

  @Test
  public void testUserCreationAndDrop() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {

          String newUser = new AdminUserFunctionalTest().routeCreateNewUser();

          // Let's get the user and the role to check that have been created

          testUserExists(newUser, true);
          testRoleExists(RoleDao.getFriendRoleName(newUser), true);

          deleteUser(newUser);
          testUserExists(newUser, false);
          testRoleExists(RoleDao.getFriendRoleName(newUser), false);

        }

      });
  }

  @Test
  public void testUserCreationWithSingleWayFriendshipAndDrop() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          String newUserToDelete = new AdminUserFunctionalTest().routeCreateNewUser();
          String newUser = new AdminUserFunctionalTest().routeCreateNewUser();
          
          makeUserFollowerOf(newUser, newUserToDelete);

          String toDeleteRoleName = RoleDao.getFriendRoleName(newUserToDelete);

          testUserHasRole(newUser, toDeleteRoleName, true);

          deleteUser(newUserToDelete);

          testUserHasRole(newUser, toDeleteRoleName, false);

        }
      });
  }

  @Test
  public void testSystemUserDeletion() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          FakeRequest request = new FakeRequest("DELETE", getRouteAddress() + "/admin");
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result result = routeAndCall(request);
          assertRoute(result, "testDelete", Status.BAD_REQUEST, null, true);

          request = new FakeRequest("DELETE", getRouteAddress() + "/" + BBConfiguration.getBaasBoxAdminUsername());
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          result = routeAndCall(request);
          assertRoute(result, "testDelete", Status.BAD_REQUEST, null, true);

          request = new FakeRequest("DELETE", getRouteAddress() + "/" + BBConfiguration.getBaasBoxUsername());
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          result = routeAndCall(request);
          assertRoute(result, "testDelete", Status.BAD_REQUEST, null, true);
        }
      });
  }



  @Test
  public void testUserCreationWithDocumentAndLinkInAndDrop() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
    String newUserToDelete = new AdminUserFunctionalTest().routeCreateNewUser();
    String newUser = new AdminUserFunctionalTest().routeCreateNewUser();

    testUserExists(newUser, true);
    testUserExists(newUserToDelete, true);
    testRoleExists(RoleDao.getFriendRoleName(newUser), true);
    testRoleExists(RoleDao.getFriendRoleName(newUserToDelete), true);

    String parentCollectionName = "posts_" + UUID.randomUUID().toString();
    String childCollectionName = "comments_" + UUID.randomUUID().toString();
    new AdminCollectionFunctionalTest().routeCreateCollection(parentCollectionName);
    new AdminCollectionFunctionalTest().routeCreateCollection(childCollectionName);

    FakeRequest fq = new FakeRequest("POST", "/document/" + parentCollectionName);
    fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
    try {
      fq = fq.withJsonBody(_om.readTree("{\"title\":\"my awesome post\",\"content\":\"that will be deleted\"}"));
    } catch (IOException e) {
      fail("Unable to parse simple json");
    }
    Result result = routeAndCall(fq);
    assertRoute(result, "testCreatePost", Status.OK, null, true);

    String postId = null;
    try {
      String content = contentAsString(result);
      postId = _om.readTree(content).get("data").get("id").asText();
    } catch (IOException e) {
      fail("Unable to parse simple json");
    }

    grantToRole("read", "registered", parentCollectionName, postId, newUser);

    fq = new FakeRequest("POST", "/document/" + childCollectionName);
    fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUserToDelete, "passw1"));
    try {
      fq = fq.withJsonBody(_om.readTree("{\"comment\":\"your post sucks\",\"rating\":5}"));
    } catch (IOException e) {
      fail("Unable to parse simple json");
    }
    result = routeAndCall(fq);
    assertRoute(result, "testCreateComment", Status.OK, null, true);
    String commentId = null;
    try {
      String content = contentAsString(result);
      commentId = _om.readTree(content).get("data").get("id").asText();
    } catch (IOException e) {
      fail("Unable to parse simple json");
    }

    grantToRole("read", "registered", childCollectionName, commentId, newUserToDelete);

    testDocumentExists(parentCollectionName, postId, true);
    testDocumentExists(childCollectionName, commentId, true);

    fq = new FakeRequest("POST", "/link/" + postId + "/comment/" + commentId);
    fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
    result = routeAndCall(fq);
    assertRoute(result, "testCreateLink", Status.OK, null, true);

    deleteUser(newUserToDelete);

    testDocumentExists(parentCollectionName, postId, true);
    testDocumentExists(childCollectionName, commentId, false);

    fq = new FakeRequest("GET", "/document/" + parentCollectionName + "/" + postId + "/comment");
    fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
    result = routeAndCall(fq);
    assertRoute(result, "testGetLink", Status.OK, "{\"result\":\"ok\",\"data\":[]", true);

        }

      });
  }

  @Test
  public void testAllowUserToBeDeletedGrantsOnDocumentAndDrop() throws Exception {
	  running(
		      getFakeApplication(),
		      new Runnable()
		      {
		        public void run()
		        {

		          String newUserToDelete = new AdminUserFunctionalTest().routeCreateNewUser();
		          String newUser = new AdminUserFunctionalTest().routeCreateNewUser();

		          // Let's get the user and the role to check that have been created

		          testUserExists(newUser, true);
		          testUserExists(newUserToDelete, true);
		          testRoleExists(RoleDao.getFriendRoleName(newUser), true);
		          testRoleExists(RoleDao.getFriendRoleName(newUserToDelete), true);

		          String collectionName = "posts_" + UUID.randomUUID().toString();
		          new AdminCollectionFunctionalTest().routeCreateCollection(collectionName);

		          FakeRequest fq = new FakeRequest("POST", "/document/" + collectionName);
		          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
		          try {
		            fq = fq.withJsonBody(_om.readTree("{\"title\":\"my awesome post\",\"content\":\"that will be deleted\"}"));
		          } catch (IOException e) {
		            fail("Unable to parse simple json");
		          }
		          Result result = routeAndCall(fq);
		          assertRoute(result, "testCreatePost", Status.OK, null, true);


		          String postId = null;
		          try {
		            String content = contentAsString(result);
		            postId = _om.readTree(content).get("data").get("id").asText();
		          } catch (IOException e) {
		            fail("Unable to parse simple json");
		          }

		          grantToUser("read", newUserToDelete, collectionName, postId,newUser);

		          deleteUser(newUserToDelete);

		          testDocumentExists(collectionName, postId, true);


		        }

		      });
  }

  @Test
  public void testUserCreationWithDocumentAndLinkOutAndDrop() throws Exception {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {

          String newUserToDelete = new AdminUserFunctionalTest().routeCreateNewUser();
          String newUser = new AdminUserFunctionalTest().routeCreateNewUser();

          // Let's get the user and the role to check that have been created

          testUserExists(newUser, true);
          testUserExists(newUserToDelete, true);
          testRoleExists(RoleDao.getFriendRoleName(newUser), true);
          testRoleExists(RoleDao.getFriendRoleName(newUserToDelete), true);

          String parentCollectionName = "posts_" + UUID.randomUUID().toString();
          String childCollectionName = "comments_" + UUID.randomUUID().toString();
          new AdminCollectionFunctionalTest().routeCreateCollection(parentCollectionName);
          new AdminCollectionFunctionalTest().routeCreateCollection(childCollectionName);

          FakeRequest fq = new FakeRequest("POST", "/document/" + parentCollectionName);
          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUserToDelete, "passw1"));
          try {
            fq = fq.withJsonBody(_om.readTree("{\"title\":\"my awesome post\",\"content\":\"that will be deleted\"}"));
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }
          Result result = routeAndCall(fq);
          assertRoute(result, "testCreatePost", Status.OK, null, true);


          String postId = null;
          try {
            String content = contentAsString(result);
            postId = _om.readTree(content).get("data").get("id").asText();
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }

          grantToRole("read", "registered", parentCollectionName, postId, newUserToDelete);

          fq = new FakeRequest("POST", "/document/" + childCollectionName);
          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
          try {
            fq = fq.withJsonBody(_om.readTree("{\"comment\":\"your post sucks\",\"rating\":5}"));
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }
          result = routeAndCall(fq);
          assertRoute(result, "testCreateComment", Status.OK, null, true);
          String commentId = null;
          try {
            String content = contentAsString(result);
            commentId = _om.readTree(content).get("data").get("id").asText();
          } catch (IOException e) {
            fail("Unable to parse simple json");
          }

          grantToRole("read", "registered", childCollectionName, commentId, newUser);

          testDocumentExists(parentCollectionName, postId, true);
          testDocumentExists(childCollectionName, commentId, true);

         
          fq = new FakeRequest("POST", "/link/" + postId + "/comment/" + commentId);
          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUserToDelete, "passw1"));
          result = routeAndCall(fq);
          assertRoute(result, "testCreateLink", Status.OK, null, true);

          deleteUser(newUserToDelete);

          testDocumentExists(parentCollectionName, postId, false);
          testDocumentExists(childCollectionName, commentId, true);

          fq = new FakeRequest("GET", "/document/" + childCollectionName + "/" + commentId + "/comment?linkDir=from");
          fq = fq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fq = fq.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(newUser, "passw1"));
          result = routeAndCall(fq);
          assertRoute(result, "testGetLink", Status.OK, "{\"result\":\"ok\",\"data\":[]", true);


        }

      });
  }

  private void testRoleExists(String friendRole, boolean exists) {
    FakeRequest request = new FakeRequest("GET", "/admin/role/" + friendRole);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = routeAndCall(request);
    if (exists) {
      assertRoute(result, "testRoleExists", Status.OK, null, true);
    } else {
      assertRoute(result, "testRoleExists", Status.NOT_FOUND, null, true);
    }

  }

  private void deleteUser(String userName) {

    FakeRequest request = new FakeRequest("DELETE", getRouteAddress() + "/" + userName);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = routeAndCall(request);
    assertRoute(result, "testDelete", Status.OK, null, true);
  }


  private void testUserExists(String newUser, boolean exists) {
    FakeRequest request = new FakeRequest("GET", getRouteAddress() + "/" + newUser);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = routeAndCall(request);
    if (exists) {
      assertRoute(result, "testUserExists", Status.OK, null, true);
    } else {
      assertRoute(result, "testUserExists", Status.NOT_FOUND, null, true);
    }

  }

  private void testUserHasRole(String user, String roleName, boolean roleExists) {
    // TODO:commented request fails with a NPE...ask @giastfader
    /*
     * FakeRequest request = new FakeRequest("GET", "/me");
     * request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
     * request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(user,"passw1");
     * Result result = routeAndCall(request);
     */
    FakeRequest request = new FakeRequest("GET", "/admin/user/" + user);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = routeAndCall(request);
    assertRoute(result, "testUserExists", Status.OK, null, true);
    try {
      String content = contentAsString(result);
      JsonNode root = _om.readTree(content);
      JsonNode jn = root.get("data").get("user").get("roles");
      Iterator<JsonNode> i = jn.iterator();
      boolean found = false;
      while (i.hasNext()) {
        JsonNode role = i.next();
        if (role.get("name").asText().equals(roleName)) {
          found = true;
          break;
        }
      }
      String message = "User should have the role " + roleName;
      if (!roleExists) {
        message = "User shouldn't have the role " + roleName;
      }
      assertTrue(message, found == roleExists);
    } catch (IOException e) {
      fail("Unable to parse simple json");
    }
  }

  protected void makeUserFollowerOf(String follower, String toFollow) {
    FakeRequest request = new FakeRequest("POST", "/follow/" + toFollow);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(follower, "passw1"));
    Result result = routeAndCall(request);
    assertRoute(result, "testFriendshipCreation", Status.CREATED, null, true);

  }

  private void testDocumentExists(String collectionName, String documentId, boolean exists) {
    FakeRequest request = new FakeRequest("GET", "/document/" + collectionName + "/" + documentId);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = routeAndCall(request);
    if (exists) {
      assertRoute(result, "testDocumentExists", Status.OK, null, true);
    } else {
      assertRoute(result, "testDocumentExists", Status.NOT_FOUND, null, true);
    }

  }

}
