import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.headers;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentAsBytes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.SimpleResult;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import core.AbstractRouteHeaderTest;
import core.AbstractTest;
import core.AbstractUsersTest;
import core.TestConfig;

public class UserListTest extends AbstractUsersTest {

  @Override
  public String getRouteAddress() {
    return ROUTE_USERS;
  }

  @Override
  public String getMethod() {
    return "GET";
  }

  @Override
  protected void assertContent(String s) {
    assertJSON("", s);

  }

  public String routeCreateNewUser(String username) {
    return routeCreateNewUser(username, true);
  }

  public String routeCreateNewUser(String username, boolean appendUUID)
  {
    String sFakeUser = username;
    if (appendUUID) {
      sFakeUser = sFakeUser + "-" + UUID.randomUUID();
    }
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

  @Test
  public void testChunkedResponse() {
    
    running
    (
      getTestServerWithChunkResponse(),
      HTMLUNIT, 
      new Callback<TestBrowser>() 
          {
        public void invoke(TestBrowser browser) 
        {
          ObjectMapper om = new ObjectMapper();
          List<String> createdUsernames = Lists.newArrayList();
          for (int i = 0; i < 10; i++) {
            createdUsernames.add(routeCreateNewUser("user" + i));
          }
          
          FakeRequest request = new FakeRequest(GET, getRouteAddress());
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result result = routeAndCall(request);
          String content = new String(chunkedContentAsBytes((SimpleResult) result));

          Map<String, String> headers = headers(result);
          assertNotNull(headers.get("Transfer-Encoding"));
          assertEquals("chunked",headers.get("Transfer-Encoding"));
          assertRoute(result, "testListUser", Status.OK, null, false);

          String queryString = "user.roles[0].name='administrator'";
          try {
            queryString = URLEncoder.encode(queryString, "UTF-8");
          } catch (UnsupportedEncodingException uee) {
            fail("unable to encode query string");
          }
          request = new FakeRequest(GET, getRouteAddress() + "?fields=user&where=" + queryString);
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);

          result = routeAndCall(request);
          content = new String(chunkedContentAsBytes((SimpleResult) result));
          assertNotNull(content);
          try{
            JsonNode jn = om.readTree(content);
            assertTrue(jn.get("data").isArray());
            assertTrue("testing that size of data (" + jn.get("data").size() + ") is 2", jn.get("data").size() == 1);
          }catch(IOException e){
            fail("unable to parse json");
          }
           
          request = new FakeRequest(GET, getRouteAddress() + "?recordsPerPage=1&page=1");
          request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);

          result = routeAndCall(request);
          content = new String(chunkedContentAsBytes((SimpleResult) result));
          assertNotNull(content);
          try {
            JsonNode jn = om.readTree(content);
            assertTrue(jn.get("data").isArray());
            assertTrue("testing that size of data (" + jn.get("data").size() + ") is 1", jn.get("data").size() == 1);
          } catch (IOException e) {
            fail("unable to parse json");
          }
        }
          }
    );
  }

}
