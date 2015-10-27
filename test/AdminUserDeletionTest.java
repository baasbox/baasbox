import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.contentAsString;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;
import static org.junit.Assert.fail;
import com.baasbox.dao.RoleDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
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
  public void testSimpleUserCreationAndDrop() throws Exception {
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
            System.out.println("CONTENT IS" + content);
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
  public void testSimpleUserCreationDocumentAndDrop() throws Exception {
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
