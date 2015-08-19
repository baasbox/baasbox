import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import play.test.TestBrowser;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;

import core.AbstractDocumentTest;
import core.TestConfig;


public class DocumentPaginationTest extends AbstractDocumentTest {

	private  String sFakeCollection;
	private int recordsToLoad=200;

	@Override
	public String getRouteAddress() {
		return DocumentCMDFunctionalTest.SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
	}

	@Override
	public String getMethod() {
		return "GET";
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
	}
	
	@Before
	public  void beforeTest(){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();
					//create some records to test pagination
					for (int i=0;i<recordsToLoad;i++) routeCreateDocument(getRouteAddress(sFakeCollection));
				}
			});
	}

	@Test 
	public void testPaginationWithChunks()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					continueOnFail(false);
					//get the total number of records
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection) + "/count","GET");
					assertServer("count", Status.OK, null, true);	
					
					String resultAsString = getResponse();
					
					int numRecords=0;
					try {
						numRecords=BBJson.mapper().readTree(resultAsString).get("data").get("count").asInt();
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
					Assert.assertTrue("Records must be "+recordsToLoad, numRecords==recordsToLoad);
					
					//ask for first 10 records
					httpRequest(getURLAddress(sFakeCollection) + "?page=0&recordsPerPage=10","GET");
					assertServer("first 10 records", Status.OK, null, true);
					resultAsString = getResponse();
					try {
						ArrayNode records = (ArrayNode) BBJson.mapper().readTree(resultAsString).get("data");
						Assert.assertTrue(records.size() == 10 );
						//check the "more" field
						BooleanNode more =  (BooleanNode) BBJson.mapper().readTree(resultAsString).get("more");
						Assert.assertTrue(more!=null && !more.isNull());
						Assert.assertTrue(more.asBoolean());
						//Assert.assertTrue("true".equals(getResponseHeaders().get("X-BB-MORE")));
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
					
					//ask for the last 10 records
					httpRequest(getURLAddress(sFakeCollection) + "?page="+((recordsToLoad/10)-1)+"&recordsPerPage=10","GET");
					assertServer("last 10 records", Status.OK, null, true);
					resultAsString = getResponse();
					
					try {
						ArrayNode records = (ArrayNode) BBJson.mapper().readTree(resultAsString).get("data");
						Assert.assertTrue(records.size() == 10 );
						//check the "more" field
						BooleanNode more =  (BooleanNode) BBJson.mapper().readTree(resultAsString).get("more");
						Assert.assertTrue(more!=null && !more.isNull());
						Assert.assertFalse(more.asBoolean());
						//Assert.assertTrue("false".equals(getResponseHeaders().get("X-BB-MORE")));
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
				}
			});
	}
	
}
