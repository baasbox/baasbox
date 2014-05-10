import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Test;

import play.libs.F.Callback;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import play.test.TestBrowser;
import core.AbstractRootTest;
import core.TestConfig;

public class RootExportTest extends AbstractRootTest {

	@Override
	public String getRouteAddress() {
		return "/root/db/export";
	}

	@Override
	public String getMethod() {
		return GET;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
	}
	
	
	
	/**
	 * This test makes a full round trip generating a db export
	 * with async Root controller method /roor/db/export (POST).
	 * 
	 * A while loop invokes the GET /root/db/export until the zip
	 * file is generated (making the test pass) or fails after 10 repetitions
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPostGetExport() throws Exception	{
		running	(
				testServer(TestConfig.SERVER_PORT,getFakeApplication()), HTMLUNIT, new Callback<TestBrowser>(){
					public void invoke(TestBrowser browser){

					
					String sAuthEnc = TestConfig.AUTH_ROOT_ENC;
					
					FakeRequest request = new FakeRequest("POST", getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					Result result = routeAndCall(request);
					assertRoute(result, "testExport", Status.ACCEPTED, null, true);
					
					String body = play.test.Helpers.contentAsString(result);
					JsonNode node = Json.parse(body);
					String fileName = node.get("data").asText();
					if(fileName==null || StringUtils.isEmpty(fileName)){
						fail("Body does not contain fileName");
					}
					
					boolean gen = false;
					int reps = 0;
					while(!gen){
						reps++;
						if(reps==11){
							fail("After "+reps+" loops the file has not been found");
						}
						FakeRequest request2 = new FakeRequest(getMethod(), getRouteAddress());
						request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request2 = request2.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						Result result2 = routeAndCall(request2);
						assertRoute(result2, "testExport", Status.OK, null, true);
						body = play.test.Helpers.contentAsString(result2);
						node = Json.parse(body);
						for(final JsonNode n : node.get("data")){
							if(n.asText().equals(fileName)){
								gen = true;
							}
						}
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							fail("Unable to sleep the thread");
						}
						
					}
					delete(fileName);
					
				}
			}
		);		
	}
	
	@Test
	public void testPostGetFileExport() throws Exception {
		running	(
			testServer(TestConfig.SERVER_PORT,getFakeApplication()), HTMLUNIT, new Callback<TestBrowser>(){
				public void invoke(TestBrowser browser){

					
					String sAuthEnc = TestConfig.AUTH_ROOT_ENC;
					
					FakeRequest request = new FakeRequest("POST", getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					Result result = routeAndCall(request);
					assertRoute(result, "testExport", Status.ACCEPTED, null, true);
					
					String body = play.test.Helpers.contentAsString(result);
					JsonNode node = Json.parse(body);
					String fileName = node.get("data").asText();
					if(fileName==null || StringUtils.isEmpty(fileName)){
						fail("Body does not contain fileName");
					}
					
					boolean gen = false;
					int reps = 0;
					while(!gen){
						reps++;
						if(reps==11){
							fail("After "+reps+" loops the file has not been found");
						}
						FakeRequest request2 = new FakeRequest(getMethod(), "/root/db/export/"+fileName);
						request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request2 = request2.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						Result result2 = route(request2);
						
						if(Helpers.status(result2) == 200){
							gen = true;
							String header = Helpers.header("Content-Type", result2);
							assertEquals("The Content-Type is wrong","application/zip",header);
							request2 = new FakeRequest(getMethod(), "/root/db/export/");
							request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request2 = request2.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							 result2 = route(request2);
							
							
						}
						
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							fail("Unable to sleep the thread");
						}
						
					}
					
					//TODO: on windows delete fails...probably because the result stream is open.
					//delete(fileName);
					
					
				}
			}
		);		
	}
	


	
		
	
	private void delete(String fileName){
		String sAuthEnc = TestConfig.AUTH_ROOT_ENC;
		FakeRequest deleteRequest = new FakeRequest(DELETE, "/root/db/export/"+fileName);
		deleteRequest = deleteRequest.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		deleteRequest = deleteRequest.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
		Result deleteResult = route(deleteRequest);
		assertTrue(Helpers.status(deleteResult)==200);
		
	}
}
