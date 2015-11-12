import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.headers;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.mvc.SimpleResult;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import core.AbstractTest;
import core.TestConfig;

public class FriendshipFunctionalChunkedTest extends AbstractTest {

  @Override
  public String getRouteAddress() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getMethod() {
    return "GET";
  }

  @Override
  protected void assertContent(String s) {
    // TODO Auto-generated method stub

  }

  @Test
  public void testFollowingChunkedResponse() throws Exception {
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
            createdUsernames.add(new AdminUserFunctionalTest().serverCreateNewUser());
          }
          
          //*let's make the first user in the list follow the others
          String first = createdUsernames.stream().findFirst().get();
          createdUsernames.stream().filter(s->!s.equals(first)).forEach(u->{
            FakeRequest request = new FakeRequest(POST, "/follow/" + u);
            request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
            request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(first, "passw1"));
            Result result = routeAndCall(request);
            assertRoute(result, "testChunkedResponseForFollowing", 201, null, false);
          });
          FakeRequest fk = new FakeRequest(GET, "/following");
          fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(first, "passw1"));
          Result result = routeAndCall(fk);
          Map<String,String> headers = headers(result);
          Assert.assertEquals("chunked",headers.get("Transfer-Encoding"));
          String content = new String(chunkedContentAsString((SimpleResult) result));
          try {
            JsonNode jn = om.readTree(content);
            final List<String> followingMe = Lists.newArrayList();
            jn.get("data").forEach(j -> {
              followingMe.add(j.get("user").get("name").asText());
            });
            Assert.assertTrue(followingMe.size() == 9);
            Assert.assertTrue(followingMe.stream().filter(u -> u.equals(first)).count() == 0);

          } catch (IOException e) {
            Assert.fail("unable to parse json");
          }
          
          

        }          

        }
    );
  }

  @Test
  public void testFollowersChunkedResponse() throws Exception {
    running(
      getTestServerWithChunkResponse(),
      HTMLUNIT,
      new Callback<TestBrowser>()
      {
        public void invoke(TestBrowser browser)
        {
          ObjectMapper om = new ObjectMapper();
          List<String> createdUsernames = Lists.newArrayList();
          for (int i = 0; i < 10; i++) {
            createdUsernames.add(new AdminUserFunctionalTest().serverCreateNewUser());
          }

          // *let's make the first user in the list follow the others
          String first = createdUsernames.stream().findFirst().get();
          createdUsernames.stream().filter(s -> !s.equals(first)).forEach(u -> {
            FakeRequest request = new FakeRequest(POST, "/follow/" + first);
            request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
            request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(u, "passw1"));
            Result result = routeAndCall(request);
            assertRoute(result, "testChunkedResponseForFollowers", 201, null, false);
          });
          FakeRequest fk = new FakeRequest(GET, "/followers");
          fk = fk.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          fk = fk.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(first, "passw1"));
          Result result = routeAndCall(fk);
          Map<String, String> headers = headers(result);
          Assert.assertEquals("chunked",headers.get("Transfer-Encoding"));
          String content = new String(chunkedContentAsBytes((SimpleResult) result));
          try {
            JsonNode jn = om.readTree(content);
            final List<String> followingMe = Lists.newArrayList();
            jn.get("data").forEach(j -> {
              followingMe.add(j.get("user").get("name").asText());
            });
            Assert.assertTrue("Who's following me size (" + followingMe.size() + ")", followingMe.size() == 9);
            Assert.assertTrue("Myself is not following me", followingMe.stream().filter(u -> u.equals(first)).count() == 0);

          } catch (IOException e) {
            Assert.fail("unable to parse json");
          }

        }

      });
  }

}
