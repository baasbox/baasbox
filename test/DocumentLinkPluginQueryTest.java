import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import core.TestConfig;
public class DocumentLinkPluginQueryTest extends BlogSampleTest{

	public static String PARENT_COLLECTION_NAME = "posts2";
	public static String CHILD_COLLECTION_NAME = "comments2";
	public static String LINK_NAME = "comment";

	ObjectMapper om = new ObjectMapper();

	private final static String TEST_CALL="test.document_link_"+ScriptTestHelpers.randomScriptName();

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

	@BeforeClass
	public static void installScript(){
		running(fakeApplication(),()->{
			try {
				DbHelper.open("1234567890", "admin", "admin");
				ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_document_link.js");
			}catch (Throwable e){
				fail(ExceptionUtils.getStackTrace(e));
			} finally {
				DbHelper.close(DbHelper.getConnection());
			}
		});
	}

	@Test
	public void testLink() {
		running(
				fakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						int minComments = 3;
						int minPosts = 1;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts,minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);
						String postWithMoreComments = ts.getPostWithMoreComments();
						String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
						int commentCount = ts.getPostToComments().get(postWithMoreComments).size();


						FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL+"?postId="+ postWithMoreComments+"&postCollection="+PARENT_COLLECTION_NAME);
						req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
						req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
						req = req.withHeader(CONTENT_TYPE,"application/json");
						Result res = route(req);
						Assert.assertEquals("GET VALUE FROM LINK PLUGIN",200,Helpers.status(res));
						String content= contentAsString(res);
						System.out.println("CONTENT:"+content);
						try {
							JsonNode node = om.readTree(content);
							String author = node.get("data").get(0).get("_author").asText();
							Assert.assertEquals(author, ts.getCommentToAuthor().get(comment));
							Assert.assertEquals(node.get("data").size(), commentCount);

						} catch (IOException e) {
							e.printStackTrace();
						}
						shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
					}

				});
	}
	
	@Test
	public void testLinkWhere() {
		running(
				fakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						int minComments = 3;
						int minPosts = 1;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts,minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);
						String postWithMoreComments = ts.getPostWithMoreComments();
						String commentId = ts.getPostToComments().get(postWithMoreComments).get(0);
						int commentCount = ts.getPostToComments().get(postWithMoreComments).size();
						
						
						
						
						FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + ts.getPostWithMoreComments() + "/comment");
				          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
				          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
				          Result r = routeAndCall(rq);
				          assertRoute(r, "get link", 200, null, false);

				          String content = contentAsString(r);
				          String commentFirstWord = null;
				          int count = 0;
				          try {
				            JsonNode node = om.readTree(content);
				            Iterator<JsonNode> comments = node.get("data").iterator();
				            while(comments.hasNext()){
				              JsonNode n = comments.next();
				              String id = n.get("id").asText();
				              String comment = n.get("comment").asText();
				              if (commentFirstWord == null) {
				                String[] split = comment.split(" ");
				                commentFirstWord = split[0];
				              }
				              if (comment.startsWith(commentFirstWord)) {
				                count++;
				              }
				              
				            }

				          } catch (IOException e) {
				            e.printStackTrace();
				          }
				          String params = null;
							params = "where=";
							try {
								params += URLEncoder.encode("comment like '<TEXT_PLACEHOLDER>%'".replace("<TEXT_PLACEHOLDER>", commentFirstWord), "UTF-8");
							} catch (UnsupportedEncodingException e1) {
								fail("Encoding error");
							}
						FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL+"?postId="+ postWithMoreComments+"&postCollection="+PARENT_COLLECTION_NAME+"&"+params	);
						req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
						req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
						req = req.withHeader(CONTENT_TYPE,"application/json");
						Result res = route(req);
						Assert.assertEquals("GET VALUE FROM LINK PLUGIN",200,Helpers.status(res));
						content= contentAsString(res);
						System.out.println("AFTER WHERE:"+content);
						try {
							JsonNode node = om.readTree(content);
							String author = node.get("data").get(0).get("_author").asText();
							Assert.assertEquals(author, ts.getCommentToAuthor().get(commentId));
							Assert.assertEquals(node.get("data").size(), count);

						} catch (IOException e) {
							e.printStackTrace();
						}
						shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
					}

				});
	}

	/*@Test
  public void testLinkNavigationMassive() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 500;
          int minPosts = 200;
          shutdownTest(false);
          TestSetup ts = prepareTest(minPosts, minComments);
          assertTrue(ts.authors.size() > 0);
          assertTrue(ts.postIds.size() > 0);
          assertTrue(ts.commentToAuthor.size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.postToComments.get(postWithMoreComments).get(0);
          int commentCount = ts.postToComments.get(postWithMoreComments).size();

          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          String content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            Assert.assertEquals(author, ts.commentToAuthor.get(comment));
            Assert.assertEquals(node.get("data").size(), commentCount);

          } catch (IOException e) {
            e.printStackTrace();
          }

          shutdownTest(true);
        }

      });
  }

  @Test
  public void testWrongQueryParams() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 3;
          int minPosts = 1;
          shutdownTest(false);
          TestSetup ts = prepareTest(minPosts, minComments);
          assertTrue(ts.authors.size() > 0);
          assertTrue(ts.postIds.size() > 0);
          assertTrue(ts.commentToAuthor.size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.postToComments.get(postWithMoreComments).get(0);
          int commentCount = ts.postToComments.get(postWithMoreComments).size();

          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment?linkDir=wrong");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 400, null, false);
          shutdownTest(true);
        }

      });
  }

  @Test
  public void testWhere() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 3;
          int minPosts = 1;
          shutdownTest(false);
          TestSetup ts = new TestSetup();
          new AdminCollectionFunctionalTest().routeCreateCollection(PARENT_COLLECTION_NAME);
          new AdminCollectionFunctionalTest().routeCreateCollection(CHILD_COLLECTION_NAME);
          int numberOfUsers = 5;
          // Create n users
          IntStream.range(0, numberOfUsers).forEach(i -> {
            ts.addAuthor(new AdminCollectionFunctionalTest().createNewUser("user" + i));
          });
          createPosts(1, ts);
          createComments(10, ts);
          String encoding = "";

          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + ts.getPostWithMoreComments() + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);

          String content = contentAsString(r);
          String commentFirstThreeWords = null;
          int count = 0;
          try {
            JsonNode node = om.readTree(content);
            Iterator<JsonNode> comments = node.get("data").iterator();
            while(comments.hasNext()){
              JsonNode n = comments.next();
              String id = n.get("id").asText();
              String comment = n.get("comment").asText();
              if (commentFirstThreeWords == null) {
                String[] split = comment.split(" ");
                commentFirstThreeWords = Joiner.on(" ").join(Lists.newArrayList(split[0], split[1], split[2]));
              }
              if (comment.indexOf(commentFirstThreeWords) > -1) {
                count++;
              }

            }

          } catch (IOException e) {
            e.printStackTrace();
          }
          try {
            encoding = "comment like '%<Text placeholder>%'".replace("<Text placeholder>", commentFirstThreeWords);
            encoding = URLEncoder.encode(encoding, "UTF-8");
          } catch (Exception e) {
            fail("encoding of query string failed");
          }
          rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + ts.getPostWithMoreComments() + "/comment?where="+encoding);
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);

            int size = node.get("data").size();
            Assert.assertEquals(size,count);
          }catch(Exception e){
            fail("Unable to parse json");
          }
          shutdownTest(true);
        }

      });
  }

  @Test
  public void testLinkNavigationMoreMassive() {
    running(
      getFakeApplication(),
      new Runnable()
      {
        public void run()
        {
          int minComments = 5000;
          int minPosts = 1000;
          shutdownTest(false);
          TestSetup ts = prepareTest(minPosts, minComments);
          assertTrue(ts.authors.size() > 0);
          assertTrue(ts.postIds.size() > 0);
          assertTrue(ts.commentToAuthor.size() > 0);

          String postWithMoreComments = ts.getPostWithMoreComments();
          String comment = ts.postToComments.get(postWithMoreComments).get(0);
          int commentCount = ts.postToComments.get(postWithMoreComments).size();
          FakeRequest rq = new FakeRequest("GET", "/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment");
          rq = rq.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
          rq = rq.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
          Result r = routeAndCall(rq);
          assertRoute(r, "get link", 200, null, false);
          String content = contentAsString(r);
          try {
            JsonNode node = om.readTree(content);
            String author = node.get("data").get(0).get("_author").asText();
            Assert.assertEquals(author, ts.commentToAuthor.get(comment));
            Assert.assertEquals(node.get("data").size(), commentCount);

          } catch (IOException e) {
            e.printStackTrace();
          }

          shutdownTest(true);
        }

      });
  }
	 */


}
