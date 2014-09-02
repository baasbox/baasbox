
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.PUT;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Before;
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

public class PushProfileTest extends AbstractTest {
		List<String> profiles;
	
		@Before
		public void beforeTest(){
			addProfiles();
		}
		
	
	
		@Test
		public void PushProfileDisabled(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						for(String profile : profiles){
							FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile+".push.profile.enable");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
							request = request.withJsonBody(getPayload(getDefaultPayload()), getMethod());
							Result result = routeAndCall(request);
							if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile request: " + request.getWrappedRequest().headers());
							if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile result: " + contentAsString(result));
							assertRoute(result, "configuration missing for the selected profile", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_CONFIG_INVALID.getDescription(), true);
						}
					}
				}
				);
		}
		
		@Test
		public void PushProfileSwitchMode(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						for(String profile : profiles){
							FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile+".push.sandbox.enable");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
							request = request.withJsonBody(getPayload("/pushDisableSandbox.json"), getMethod());
							Result result = routeAndCall(request);
							if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode request: " + request.getWrappedRequest().headers());
							if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode result: " + contentAsString(result));
							assertRoute(result, "configuration missing for the selected mode", Status.BAD_REQUEST, CustomHttpCode.PUSH_SWITCH_EXCEPTION.getDescription(), true);
						}
					}
				}
				);
		}
		
		@Test
		public void PushProfileSend(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;

						// Too many profiles

						FakeRequest request = new FakeRequest("POST", "/push/message/"+sFakeUser);
						Logger.debug("Route: " + request);
						Logger.debug("Users: " + sFakeUser);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushTooManyProfiles.json"), getMethod());
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles request: " + request.getWrappedRequest().headers());
						//if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles result: " + contentAsString(result));
						assertRoute(result, "error with send, too many profiles", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_ARRAY_EXCEPTION.getDescription(), true);

						continueOnFail(true);

						// Profile not enabled
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayload.json"), getMethod());
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						continueOnFail(true);
						
						// Profile not supported
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileNotSupported.json"), getMethod());
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile not supported", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_INVALID.getDescription(), true);
						
						
							
							
					}
				}
				);
		}
		
		public void addProfiles(){
			profiles = new ArrayList<String>();
			profiles.add("profile1");
			profiles.add("profile2");
			profiles.add("profile3");
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
			return "/pushEnableProfile.json";
		}
		
}
