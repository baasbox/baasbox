
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

public abstract class PushProfileAbstractTest extends AbstractTest {
		private List<String> profiles;
		
		{
			profiles = new ArrayList<String>();
			profiles.add("profile2");
			profiles.add("profile3");
		}
	
		protected abstract int getProfile1DisabledReturnCode();
		protected abstract int getProfile1SwitchReturnCode();
		
	
		@Test
		public void PushProfileDisabledProfile1(){
			running
			(
				getFakeApplication(), 
				new Runnable() 	{
					public void run() 	{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						String profile= "profile1";
						FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile+".push.profile.enable");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload(getDefaultPayload()), getMethod());
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile result: " + contentAsString(result));
						assertRoute(result, "configuration missing for the selected profile ("+profile+")", getProfile1DisabledReturnCode(), null, false);
					}
				}
			);
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
							assertRoute(result, "configuration missing for the selected profile ("+profile+")", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_CONFIG_INVALID.getDescription(), true);
						}
					}
				}
				);
		}
		
		
		@Test
		public void PushProfileSwitchModeProfile1(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						String profile= "profile1";
						FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile+".push.sandbox.enable");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushDisableSandbox.json"), getMethod());
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode result: " + contentAsString(result));
						assertRoute(result, "configuration missing for the selected mode ("+profile+")", getProfile1SwitchReturnCode(), null, false);
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
							assertRoute(result, "configuration missing for the selected mode ("+profile+")", Status.BAD_REQUEST, CustomHttpCode.PUSH_SWITCH_EXCEPTION.getDescription(), true);
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
						request = request.withJsonBody(getPayload("/pushPayloadTooManyProfiles.json"), play.test.Helpers.POST);
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles result: " + contentAsString(result));
						assertRoute(result, "error with send, too many profiles", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_ARRAY_EXCEPTION.getDescription(), true);

						continueOnFail(true);

						// Profile not enabled, with profile specified in Payload
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabledWithProfileSpecified request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled, with profile specified in Payload", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						continueOnFail(true);
						
						// Profile not enabled, without profile specified in Payload
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithoutProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabledWithoutProfileSpecified request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled, without profile specified in Payload", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						continueOnFail(true);
						
						// Profile not supported
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileNotSupported.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile not supported", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_INVALID.getDescription(), true);
						
						//Push with key message different from String
						
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithMessageDifferentFromString.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithMessageDifferentFromString request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithMessageDifferentFromString result: " + contentAsString(result));
						assertRoute(result, "error with send, value message is not a String", Status.BAD_REQUEST, "Message MUST be a String", true);
							
						// Value profiles different from array
						
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithValueProfilesDifferentFromArray.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithValueProfilesDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithValueProfilesDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, value profiles is not an array", Status.BAD_REQUEST, "Profiles MUST be an Array", true);
						
					}
				}
				);
		}
		
		@Test
		public void PushProfileSendToUsers(){
			running
			(
				getFakeApplication(), 
				new Runnable() 
				{
					public void run() 
					{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;

						// Users key empty

						FakeRequest request = new FakeRequest("POST", "/push/message");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadTooManyProfiles.json"), play.test.Helpers.POST);
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueEmpty request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueEmpty result: " + contentAsString(result));
						assertRoute(result, "error with send, key users empty", Status.BAD_REQUEST, CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getDescription(), true);
							
						// Users value different from array
						
						request = new FakeRequest("POST", "/push/message");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithValueUsersDifferentFromArray.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, key users empty", Status.BAD_REQUEST, "Users MUST be an Array", true);
						
						// Profiles value must be expressed in numbers
						
						request = new FakeRequest("POST", "/push/message");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfilesValueExpressedInString.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, key users empty", Status.BAD_REQUEST, "Profiles MUST be express as number", true);
						
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
			return "/pushPayloadEnableProfile.json";
		}
		
}
