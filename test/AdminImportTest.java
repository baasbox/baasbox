import static org.junit.Assert.assertTrue;
import static play.test.Helpers.POST;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.codehaus.jackson.JsonNode;
import org.junit.*;
import play.libs.Json;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;

public class AdminImportTest extends AbstractTest {

	@Override
	public String getRouteAddress() {
		return "/admin/db/import";
	}

	@Override
	public String getMethod() {
		return POST;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
	}
	
	
	
	/**
	 * Testing import functionality...while the db is in import mode
	 * the db shouldn't be accessible
	 * @throws Exception
	 */
	@Test
	public void testPostGetImport() throws Exception
	{
		running
		(
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					
					String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
					
					FakeRequest request = new FakeRequest("POST", getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
					JsonNode importJson = getPayload("adminImportJson.json");
					String body = Json.stringify(importJson); 
					assertTrue(body!=null && body.length()>0);
					request.withJsonBody(importJson);
					Result result = routeAndCall(request);
					assertRoute(result, "testImport", Status.ACCEPTED, null, true);
					try {
						Thread.sleep(25000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		);		
	}
	
	
	
	
}
