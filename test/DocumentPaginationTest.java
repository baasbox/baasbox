import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;

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
	public void testPagination()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					//get the total number of records
					FakeRequest request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "/count");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "count", Status.OK, null, true);
					String resultAsString = contentAsString(result);
					int numRecords=0;
					try {
						numRecords=BBJson.mapper().readTree(resultAsString).get("data").get("count").asInt();
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
					Assert.assertTrue("Records must be "+recordsToLoad, numRecords==recordsToLoad);
					//ask for first 10 records
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "?page=0&recordsPerPage=10");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "first 10 records", Status.OK, null, true);
					resultAsString = contentAsString(result);
					try {
						ArrayNode records = (ArrayNode) BBJson.mapper().readTree(resultAsString).get("data");
						Assert.assertTrue(records.size() == 10 );
						//check the "more" field
						BooleanNode more =  (BooleanNode) BBJson.mapper().readTree(resultAsString).get("more");
						Assert.assertTrue(more!=null && !more.isNull());
						Assert.assertTrue(more.asBoolean());
						Assert.assertTrue("true".equals(Helpers.header("X-BB-MORE", result)));
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
					
					//ask for the last 10 records
					request = new FakeRequest(GET, getRouteAddress(sFakeCollection) + "?page="+((recordsToLoad/10)-1)+"&recordsPerPage=10");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "last 10 records", Status.OK, null, true);
					resultAsString = contentAsString(result);
					try {
						ArrayNode records = (ArrayNode) BBJson.mapper().readTree(resultAsString).get("data");
						Assert.assertTrue(records.size() == 10 );
						//check the "more" field
						BooleanNode more =  (BooleanNode) BBJson.mapper().readTree(resultAsString).get("more");
						Assert.assertTrue(more!=null && !more.isNull());
						Assert.assertFalse(more.asBoolean());
						Assert.assertTrue("false".equals(Helpers.header("X-BB-MORE", result)));
					} catch (IOException e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					} 
				}
			});
	}

}
