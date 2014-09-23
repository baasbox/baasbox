
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.PUT;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Test;

import play.Logger;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;

import com.baasbox.controllers.Admin;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.service.push.providers.GCMServer;

import core.AbstractTest;
import core.TestConfig;

public class PushGcmApiKeyTest extends AbstractTest {

		@Test
		public void PushGcmApiKey(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;

							FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/profile1.sandbox.android.api.key");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
							request = request.withJsonBody(getPayload(getDefaultPayload()), getMethod());
							Result result = routeAndCall(request);
							if (Logger.isDebugEnabled()) Logger.debug("testSetApiKey request: " + request.getWrappedRequest().headers());
							if (Logger.isDebugEnabled()) Logger.debug("testSetApiKey result: " + contentAsString(result));
							assertRoute(result, "testSetApiKey not valid", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_INVALID_APIKEY.getDescription(), true);
						
					}
				}
				);
		}

		@Override
		public String getRouteAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getMethod() {
			return PUT;
		}

		@Override
		protected void assertContent(String s) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public String getDefaultPayload()
		{
			return "/adminSetApiKey.json";
		}
		
}
