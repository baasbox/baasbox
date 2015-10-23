import static play.test.Helpers.DELETE;
import static play.test.Helpers.POST;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.contentAsString;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Test;

import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.Helpers;
import utils.LoremIpsum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentLinkQueryTest extends AbstractDocumentTest {

  public static String PARENT_COLLECTION_NAME = "posts2";
  public static String CHILD_COLLECTION_NAME = "comments2";
  public static String LINK_NAME = "comment";


  @Override
  public String getRouteAddress() {
    return "/admin/collection/" + PARENT_COLLECTION_NAME;
  }


  @Override
  public String getMethod() {
    return "POST";
  }


  @Override
  protected void assertContent(String s) {
    // TODO Auto-generated method stub

  }

  @Test
  public void testLinkNavigation() {
    running(
      getFakeApplication(), 
      new Runnable() 
      {
        public void run() 
        {
          int minComments = 3;
          int minPosts = 1;
          shutdownTest(false);
          TestSetup ts = prepareTest(minPosts,minComments);
          assertTrue(ts.authors.size() > 0);
          assertTrue(ts.postIds.size() > 0);
          assertTrue(ts.commentToAuthor.size() > 0);
          testLinks(ts);
          // shutdownTest(true);
        }

      });
  }

  protected void shutdownTest(boolean check) {
    FakeRequest fr = new FakeRequest(DELETE, "/admin/collection/" + CHILD_COLLECTION_NAME);
    fr = fr.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fr = fr.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result result = route(fr);
    if (check) {
      assertRoute(result, "routeDropCollection." + CHILD_COLLECTION_NAME, Status.OK, null, false);
    }
    fr = new FakeRequest(DELETE, "/admin/collection/" + PARENT_COLLECTION_NAME);
    fr = fr.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    fr = fr.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    result = route(fr);
    if (check) {
      assertRoute(result, "routeDropCollection." + PARENT_COLLECTION_NAME, Status.OK, null, false);
    }
  }

  private void testLinks(TestSetup ts) {

    String biggestPost = ts.postToComments.keySet().iterator().next();
    int maxPosts = ts.postToComments.get(biggestPost).size();
    for (String k : ts.postToComments.keySet()) {
      if (ts.postToComments.get(k).size() > maxPosts) {
        maxPosts = ts.postToComments.get(k).size();
        biggestPost = k;
      }
    }
    String comment = ts.postToComments.get(biggestPost).get(0);
    FakeRequest rq = new FakeRequest("GET", "/document/" + CHILD_COLLECTION_NAME + "/" + comment + "/comment");
    rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
    Result r = routeAndCall(rq);
    assertRoute(r, "get link", 200, null, false);
    String content = contentAsString(r);
    System.out.println("QUERY LINK RESULT IS " + content);
  }

  private TestSetup prepareTest(int minPosts, int minComments) {
    Random r = new Random();
    TestSetup ts = new TestSetup();
    new AdminCollectionFunctionalTest().routeCreateCollection(PARENT_COLLECTION_NAME);
    new AdminCollectionFunctionalTest().routeCreateCollection(CHILD_COLLECTION_NAME);
    int numberOfUsers = r.nextInt(100);
    // Create n users
    IntStream.range(0, numberOfUsers).forEach(i -> {
      ts.addAuthor(new AdminCollectionFunctionalTest().createNewUser("user" + i));
    });
    // create n post with random author
    int numberOfPosts = r.nextInt(minPosts) + minPosts;

    createPosts(numberOfPosts, ts);
    int numberOfComments = r.nextInt(minComments) + minComments;
    createComments(numberOfComments, ts);
    return ts;
  }

  private void createComments(int numberOfComments, TestSetup ts) {
    LoremIpsum lr = new LoremIpsum();
    Random r = new Random();
    ObjectMapper om = new ObjectMapper();
    IntStream.range(0, numberOfComments).forEach(i -> {

      int postIdx = r.nextInt(ts.postIds.size());
      String postId = ts.postIds.get(postIdx);

      int commentAuthorIdx = r.nextInt(ts.authors.size());
      String commentAuthor = ts.authors.get(commentAuthorIdx);


      FakeRequest request = new FakeRequest(POST, "/document/" + CHILD_COLLECTION_NAME);
      request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
      request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(commentAuthor, "passw1"));

      String content = lr.getWords(r.nextInt(10) + 10, r.nextInt(30));

      try {
        request = request.withJsonBody(om.readTree("{\"comment\":\"" + content + "\"}"));
      } catch (Exception e) {
        // Nothing to do;
    }
    Result result = routeAndCall(request);
    String commentContent = Helpers.contentAsString(result);

    try {
      String rid = om.readTree(commentContent).get("data").get("id").asText();
      ts.addCommentId(postId, rid, commentAuthor);
      createLink("comment", postId, rid, commentAuthor);
    } catch (Exception e) {
      // Nothing to do
    }


    });
  }

  private void createLink(String linkName, String postId, String rid, String commentAuthor) {
    FakeRequest request = new FakeRequest(POST, "/link/" + rid + "/" + linkName + "/" + postId);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(commentAuthor, "passw1"));
    Result result = routeAndCall(request);
    assertRoute(result, "createLink", 200, null, false);
  }

  private void createPosts(int numberOfPosts, TestSetup ts) {
    LoremIpsum lr = new LoremIpsum();
    Random r = new Random();
    ObjectMapper om = new ObjectMapper();
    IntStream.range(0, numberOfPosts).forEach(i -> {
      int authorIdx = r.nextInt(ts.authors.size());
      String author = ts.authors.get(authorIdx);
      FakeRequest request = new FakeRequest(POST, "/document/" + PARENT_COLLECTION_NAME);
      request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
      request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(author, "passw1"));

      String title = UUID.randomUUID().toString() + " from " + author;
      String content = lr.getWords(r.nextInt(10) + 10, r.nextInt(30));

      try {
        request = request.withJsonBody(om.readTree("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}"));
      } catch (Exception e) {
        // Nothing to do;
    }
      Result result = routeAndCall(request);
    assertRoute(result, "createPost", 200, null, false);

      String postContent = Helpers.contentAsString(result);
    try {
      String rid = om.readTree(postContent).get("data").get("id").asText();
      ts.addPostId(rid);
      ts.addPostAndAuthor(author, rid);
      grant("read", "registered", PARENT_COLLECTION_NAME, rid, author);
    } catch (Exception e) {
    }

    });
  }

  private void grant(String action, String role, String collection, String objectRid, String author) {
    FakeRequest request = new FakeRequest("PUT", "/document/" + collection + "/" + objectRid + "/" + action + "/role/" + role);
    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(author, "passw1"));
    Result result = routeAndCall(request);
    assertRoute(result, "grantPostToRegistered", 200, null, false);
  }

  static class TestSetup {
    int createdPosts;
    int createdComments;
    List<String> authors;
    List<String> postIds;
    Map<String, List<String>> authorToPosts;
    Map<String, String> postToAuthors;
    Map<String, List<String>> postToComments;
    Map<String, String> commentToAuthor;

    void addAuthor(String authorUsername) {
      if (this.authors == null) {
        this.authors = Lists.newArrayList();
      }
      this.authors.add(authorUsername);
    }

    public void addCommentId(String postId, String commentId, String author) {
      if (this.postToComments == null) {
        this.postToComments = Maps.newHashMap();
      }
      if (postToComments.containsKey(postId)) {
        List<String> comments = postToComments.get(postId);
        comments.add(commentId);
        postToComments.put(postId, comments);
      } else {
        postToComments.put(postId, Lists.newArrayList(commentId));
      }
      if (commentToAuthor == null) {
        commentToAuthor = Maps.newHashMap();
      }
      commentToAuthor.put(commentId, author);

    }

    public void addPostAndAuthor(String author, String rid) {
      if (this.authorToPosts == null) {
        authorToPosts = Maps.newHashMap();
      }
      if (authorToPosts.containsKey(author)) {
        List<String> posts = authorToPosts.get(author);
        posts.add(rid);
        authorToPosts.put(author, posts);
      } else {
        authorToPosts.put(author, Lists.newArrayList(rid));
      }
      if (postToAuthors == null) {
        postToAuthors = Maps.newHashMap();
      }
      postToAuthors.put(rid, author);
    }

    public void addPostId(String rid) {
      if (this.postIds == null) {
        this.postIds = Lists.newArrayList();
      }
      this.postIds.add(rid);

    }

  }
}
