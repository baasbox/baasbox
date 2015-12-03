import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.running;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.libs.F.Callback;
import play.test.TestBrowser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.TestConfig;

public class LinkToUserIssue907Test extends BlogSampleTest {

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
	public void createLinkToUser(){
		running(
				getTestServer(), 
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
						resetHeaders();
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(author, "passw1"));
						httpRequest(TestConfig.SERVER_URL+"/me", "GET");
						String meResponse = getResponse();
						try{
							JsonNode res = om.readTree(meResponse);
							String userId = res.get("data").get("id").asText();
							
							httpRequest(TestConfig.SERVER_URL+"/link/" + postWithMoreComments + "/theauthor/" + userId, "POST");
							httpRequest(TestConfig.SERVER_URL+"/link/" + postWithMoreComments + "/theauthor/" + userId, "POST");
							
							String linkResponse = getResponse();
							
							String linkId = null;
							try{
								JsonNode jn = om.readTree(linkResponse);
								linkId = jn.get("data").get("id").asText();
							}catch(Exception e){
								fail();
							}
									
							httpRequest(TestConfig.SERVER_URL+ "/link/"+linkId,"DELETE");
							
							Map<String,String> params = new HashMap<String,String>();
							
							
							params.put("fields","_links.out(\"theauthor\").size() as countLinks");
							params.put("where","id = '"+postWithMoreComments+"'");
							
							StringBuilder sb = new StringBuilder();
							for (String key : params.keySet()) {
								sb.append(URLEncoder.encode(key, "UTF-8"));
								sb.append("=");
								sb.append(URLEncoder.encode(params.get(key), "UTF-8"));
								sb.append("&");	
							}
							
							httpRequest(TestConfig.SERVER_URL+"/document/" + PARENT_COLLECTION_NAME+"?"+sb.toString(), "GET");
							int count = 0;
							res = om.readTree(getResponse());
							count = res.get("data").get("countLinks").asInt();
							assertEquals(1,count);
							//httpRequest(TestConfig.SERVER_URL+ "/document/"+PARENT_COLLECTION_NAME+"/"+postWithMoreComments,"DELETE");
						}catch(Exception e){
							e.printStackTrace();
							fail();
						}
								
						
					}});
	}
}
