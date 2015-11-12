import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import core.TestConfig;

public class DocumentLinkChunkedQueryTest extends BlogSampleTest {

	public static String PARENT_COLLECTION_NAME = "posts2";
	public static String CHILD_COLLECTION_NAME = "comments2";
	public static String LINK_NAME = "comment";

	ObjectMapper om = new ObjectMapper();

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
	public void testLinkingNavigationWithFiles(){
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						int minComments = 3;
						int minPosts = 1;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts,minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);
						String postWithMoreComments = ts.getPostWithMoreComments();
						String author = ts.getPostToAuthors().get(postWithMoreComments);
						//Let's create a link with two files
						String firstFileId = createFile("/logo_baasbox_lp.png", "image/png",author);
						if(StringUtils.isEmpty(firstFileId)) fail();
						String secondFileId = createFile("/logo_baasbox_lp.png", "image/png",author);
						if(StringUtils.isEmpty(secondFileId)) fail();
						createLink("attachment", postWithMoreComments, firstFileId, author);
						createLink("attachment", postWithMoreComments, secondFileId, author);
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/attachment","GET");
						assertServer("get link", 200, null, false);
						String content = getResponse();
						try {
							JsonNode node = om.readTree(content);
							Assert.assertEquals(node.get("data").size(), 2);
							shutdownTest(true, PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);

						}catch(Exception e){
							fail();
						}
					}
				});

	}

	@Test
	public void testLinkingNavigationWithMixedDocumentAndFiles(){
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						int minComments = 3;
						int minPosts = 1;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts,minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);
						String postWithMoreComments = ts.getPostWithMoreComments();
						String postWithLessComments = ts.getPostWithLessComments();
						String author = ts.getPostToAuthors().get(postWithMoreComments);
						//Let's create a link with two files
						String firstFileId = createFile("/logo_baasbox_lp.png", "image/png",author);
						if(StringUtils.isEmpty(firstFileId)) fail();
						createLink("attachment", postWithMoreComments, firstFileId, author);

						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/attachment","GET");
						assertServer("get link", 200, null, false);
						String content = getResponse();
						try {
							JsonNode node = om.readTree(content);
							Assert.assertEquals(node.get("data").size(), 1);
						}catch(Exception e){
							fail("Unable to parse json");
						}
						createLink("attachment", postWithMoreComments, postWithLessComments, author);
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/attachment","GET");
						assertServer("get link 2", 200, null, false);
						content = getResponse();
						try {
							JsonNode node = om.readTree(content);
							Assert.assertEquals(node.get("data").size(), 2);
						}catch(Exception e){
							fail();
						}
					}
				});


	}

	private String createFile(String fileName,String type,String author){
		Map<String, String> mParametersFile = new HashMap<String, String>();
		mParametersFile.put("attachedData", getPayload("/adminAssetCreateMeta.json").toString());
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(author, "passw1"));
		setMultipartFormData();
		setFile("/logo_baasbox_lp.png", "image/png");
		httpRequest(TestConfig.SERVER_URL+"/file", "POST", mParametersFile);
		assertServer("createFile",201,  null,false);
		try {
			return BBJson.mapper().readTree(getResponse()).get("data").get("id").asText();
		} catch (IOException e) {
			return null;
		}
	}

	@Test
	public void testLinkNavigation() {
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
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
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment","GET");
						assertServer("get link", 200, null, false);
						String content = getResponse();

						try {
							JsonNode node = om.readTree(content);
							String author = node.get("data").get(0).get("_author").asText();
							Assert.assertEquals(author, ts.getCommentToAuthor().get(comment));
							Assert.assertEquals(node.get("data").size(), commentCount);

						} catch (IOException e) {
							e.printStackTrace();
						}
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + CHILD_COLLECTION_NAME + "/" + comment + "/comment?linkDir=from","GET");
						assertServer("get link from", 200, null, false);
						content = getResponse();

						try {
							JsonNode node = om.readTree(content);
							String author = node.get("data").get(0).get("_author").asText();
							String id = node.get("data").get(0).get("id").asText();
							Assert.assertEquals(author, ts.getPostToAuthors().get(postWithMoreComments));
							Assert.assertEquals(node.get("data").size(), 1);
							Assert.assertEquals(postWithMoreComments, id);
						} catch (IOException e) {
							fail("Unable to parse json");
						}
						shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
					}

				});
	}

	@Test
	public void testLinkNavigationMassive() {
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						int minComments = 500;
						int minPosts = 200;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);

						String postWithMoreComments = ts.getPostWithMoreComments();
						String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
						int commentCount = ts.getPostToComments().get(postWithMoreComments).size();

						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment","GET");
						assertServer("get link", 200, null, false);
						String content = getResponse();
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
	public void testWrongQueryParams() {
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						int minComments = 3;
						int minPosts = 1;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);

						String postWithMoreComments = ts.getPostWithMoreComments();
						String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
						int commentCount = ts.getPostToComments().get(postWithMoreComments).size();

						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" + postWithMoreComments + "/comment?linkDir=wrong","GET");
						assertServer("get link", 400, null, false);
						shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
					}

				});
	}

	@Test
	public void testWhere() {
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = new TestSetup();
						new AdminCollectionFunctionalTest().routeCreateCollection(PARENT_COLLECTION_NAME);
						new AdminCollectionFunctionalTest().routeCreateCollection(CHILD_COLLECTION_NAME);
						int numberOfUsers = 5;
						// Create n users
						IntStream.range(0, numberOfUsers).forEach(i -> {
							ts.addAuthor(new AdminCollectionFunctionalTest().createNewUser("user" + i));
						});
						createPosts(1, ts,PARENT_COLLECTION_NAME);
						createComments(10, ts,CHILD_COLLECTION_NAME);
						String encoding = "";
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" +  ts.getPostWithMoreComments() + "/comment","GET");
						assertServer("get link", 200, null, false);
						String content = getResponse();
						Map<String,List<String>> headers = getResponseHeaders();
						Assert.assertEquals("chunked",headers.get("Transfer-Encoding").get(0));
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
							e.printStackTrace();
							fail("encoding of query string failed:"+e.getMessage());
						}
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" +  ts.getPostWithMoreComments() + "/comment?where="+encoding,"GET");
						assertServer("get link", 200, null, false);
						headers = getResponseHeaders();
						Assert.assertEquals("chunked",headers.get("Transfer-Encoding").get(0));
						 content = getResponse();
						
						try {
							JsonNode node = om.readTree(content);

							int size = node.get("data").size();
							Assert.assertEquals(size,count);
						}catch(Exception e){
							fail("Unable to parse json");
						}
						shutdownTest(true,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
					}

				});
	}

	@Test
	public void testLinkNavigationMoreMassive() {
		running(
				getTestServerWithChunkResponse(), 
				HTMLUNIT, 
				new Callback<TestBrowser>() 
				{
					public void invoke(TestBrowser browser) 
					{
						int minComments = 1000;
						int minPosts = 500;
						shutdownTest(false,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						TestSetup ts = prepareTest(minPosts, minComments,PARENT_COLLECTION_NAME,CHILD_COLLECTION_NAME);
						assertTrue(ts.getAuthors().size() > 0);
						assertTrue(ts.getPostIds().size() > 0);
						assertTrue(ts.getCommentToAuthor().size() > 0);

						String postWithMoreComments = ts.getPostWithMoreComments();
						String comment = ts.getPostToComments().get(postWithMoreComments).get(0);
						int commentCount = ts.getPostToComments().get(postWithMoreComments).size();
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME + "/" +  ts.getPostWithMoreComments() + "/comment","GET");
						assertServer("get link", 200, null, false);
						Map<String,List<String>> headers = getResponseHeaders();
						Assert.assertEquals("chunked",headers.get("Transfer-Encoding").get(0));
						String content = getResponse();
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








}
