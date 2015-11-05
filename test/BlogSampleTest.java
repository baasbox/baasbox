

import static play.test.Helpers.DELETE;
import static play.test.Helpers.POST;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import utils.LoremIpsum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import core.AbstractTest;
import core.TestConfig;

public abstract class BlogSampleTest extends AbstractTest {

	@Override
	public abstract String getRouteAddress();

	@Override
	public abstract String getMethod();

	@Override
	protected abstract void assertContent(String s);
	
	protected void shutdownTest(boolean check,String...collections) {
		for (String coll : collections) {
			 FakeRequest fr = new FakeRequest(DELETE, "/admin/collection/" + coll);
			    fr = fr.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
			    fr = fr.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
			    Result result = route(fr);
			    if (check) {
			      assertRoute(result, "routeDropCollection." + coll, Status.OK, null, false);
			    }
		}
	   
	  }
	
	TestSetup prepareTest(int minPosts, int minComments,String postCollection,String commentCollection) {
	    Random r = new Random();
	    TestSetup ts = new TestSetup();
	    new AdminCollectionFunctionalTest().routeCreateCollection(postCollection);
	    new AdminCollectionFunctionalTest().routeCreateCollection(commentCollection);
	    int numberOfUsers = r.nextInt(100) + 1;
	    // Create n users
	    IntStream.range(0, numberOfUsers).forEach(i -> {
	      ts.addAuthor(new AdminCollectionFunctionalTest().createNewUser("user" + i));
	    });
	    // create n post with random author
	    int numberOfPosts = r.nextInt(minPosts) + minPosts;

	    createPosts(numberOfPosts, ts,postCollection);
	    int numberOfComments = r.nextInt(minComments) + minComments;
	    createComments(numberOfComments, ts,commentCollection);
	    return ts;
	  }

	   void createComments(int numberOfComments, TestSetup ts,String collectionName) {
	    LoremIpsum lr = new LoremIpsum();
	    Random r = new Random();
	    ObjectMapper om = new ObjectMapper();
	    IntStream.range(0, numberOfComments).forEach(i -> {

	      int postIdx = r.nextInt(ts.getPostIds().size());
	      String postId = ts.getPostIds().get(postIdx);

	      int commentAuthorIdx = r.nextInt(ts.getAuthors().size());
	      String commentAuthor = ts.getAuthors().get(commentAuthorIdx);


	      FakeRequest request = new FakeRequest(POST, "/document/" + collectionName);
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
	    FakeRequest request = new FakeRequest(POST, "/link/" + postId + "/" + linkName + "/" + rid);
	    request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
	    request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(commentAuthor, "passw1"));
	    Result result = routeAndCall(request);
	    assertRoute(result, "createLink", 200, null, false);
	  }

	  void createPosts(int numberOfPosts, TestSetup ts,String collectionName) {
	    LoremIpsum lr = new LoremIpsum();
	    Random r = new Random();
	    ObjectMapper om = new ObjectMapper();
	    IntStream.range(0, numberOfPosts).forEach(i -> {
	      int authorIdx = r.nextInt(ts.getAuthors().size());
	      String author = ts.getAuthors().get(authorIdx);
	      FakeRequest request = new FakeRequest(POST, "/document/" + collectionName);
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
	      grantToRole("read", "registered", collectionName, rid, author);
	    } catch (Exception e) {
	    }

	    });
	  }
	public static class TestSetup {
	    int createdPosts;
	    int createdComments;
	    private List<String> authors;
	    private List<String> postIds;
	    Map<String, List<String>> authorToPosts;
	    private Map<String, String> postToAuthors;
	    private Map<String, List<String>> postToComments;
	    private Map<String, String> commentToAuthor;

	    public void addAuthor(String authorUsername) {
	      if (this.getAuthors() == null) {
	        this.setAuthors(Lists.newArrayList());
	      }
	      this.getAuthors().add(authorUsername);
	    }

	    public void addCommentId(String postId, String commentId, String author) {
	      if (this.getPostToComments() == null) {
	        this.setPostToComments(Maps.newHashMap());
	      }
	      if (getPostToComments().containsKey(postId)) {
	        List<String> comments = getPostToComments().get(postId);
	        comments.add(commentId);
	        getPostToComments().put(postId, comments);
	      } else {
	        getPostToComments().put(postId, Lists.newArrayList(commentId));
	      }
	      if (getCommentToAuthor() == null) {
	        setCommentToAuthor(Maps.newHashMap());
	      }
	      getCommentToAuthor().put(commentId, author);

	    }

	    public String getPostWithMoreComments() {
	      String biggestPost = this.getPostToComments().keySet().iterator().next();
	      int maxPosts = this.getPostToComments().get(biggestPost).size();
	      for (String k : this.getPostToComments().keySet()) {
	        if (this.getPostToComments().get(k).size() > maxPosts) {
	          maxPosts = this.getPostToComments().get(k).size();
	          biggestPost = k;
	        }
	      }
	      return biggestPost;
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
	      if (getPostToAuthors() == null) {
	        setPostToAuthors(Maps.newHashMap());
	      }
	      getPostToAuthors().put(rid, author);
	    }

	    public void addPostId(String rid) {
	      if (this.getPostIds() == null) {
	        this.setPostIds(Lists.newArrayList());
	      }
	      this.getPostIds().add(rid);

	    }

		public List<String> getAuthors() {
			return authors;
		}

		public void setAuthors(List<String> authors) {
			this.authors = authors;
		}

		public List<String> getPostIds() {
			return postIds;
		}

		public void setPostIds(List<String> postIds) {
			this.postIds = postIds;
		}

		public Map<String, String> getCommentToAuthor() {
			return commentToAuthor;
		}

		public void setCommentToAuthor(Map<String, String> commentToAuthor) {
			this.commentToAuthor = commentToAuthor;
		}

		public Map<String, List<String>> getPostToComments() {
			return postToComments;
		}

		public void setPostToComments(Map<String, List<String>> postToComments) {
			this.postToComments = postToComments;
		}

		public Map<String, String> getPostToAuthors() {
			return postToAuthors;
		}

		public void setPostToAuthors(Map<String, String> postToAuthors) {
			this.postToAuthors = postToAuthors;
		}

	  }
}
