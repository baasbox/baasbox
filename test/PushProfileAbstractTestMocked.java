
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

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.Admin;
import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.service.push.providers.GCMServer;

import core.AbstractTest;
import core.TestConfig;

public abstract class PushProfileAbstractTestMocked extends AbstractTest {
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
						String profile1= "profile1";
						FakeRequest request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile1+".push.profile.enable");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload(getDefaultPayload()), getMethod());
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile result: " + contentAsString(result));
						assertRoute(result, "configuration missing for the selected profile ("+profile1+")", getProfile1DisabledReturnCode(), null, false);

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
	public void PushProfileMockedOldApi(){
		running
		(
				getFakeApplication(), 
				new Runnable() 	{
					public void run() 	{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						FakeRequest request;
						Result result;
			
						continueOnFail(true);
						
						String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();

						//OLD API	
						
						// Too many profiles

						request = new FakeRequest("POST", "/push/message/"+sFakeUser);
						Logger.debug("Route: " + request);
						Logger.debug("Users: " + sFakeUser);
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadTooManyProfiles.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithTooManyProfiles result: " + contentAsString(result));
						assertRoute(result, "error with send, too many profiles", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription(), true);

						continueOnFail(true);

						// Profile not supported
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileNotSupported.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotSupported result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile not supported", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription(), true);

						// Profile NOT Array of String
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileNotSupported.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotArrayString request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileNotArrayString result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile are not an Array of String", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription(), true);

						//Push with key message different from String

						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithMessageDifferentFromString.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithMessageDifferentFromString request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithMessageDifferentFromString result: " + contentAsString(result));
						assertRoute(result, "error with send, value message is not a String", Status.BAD_REQUEST, CustomHttpCode.PUSH_MESSAGE_INVALID.getDescription(), true);

						// Value profiles different from array

						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithValueProfilesDifferentFromArray.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithValueProfilesDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithValueProfilesDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, value profiles is not an array", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription(), true);
					}
				}
				);
	}
	
	
	
	
	@Test
	public void PushProfileMockedNewApi(){
		running
		(
				getFakeApplication(), 
				new Runnable() 	{
					public void run() 	{
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						FakeRequest request;
						Result result;
			
						continueOnFail(true);
						
						String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();


						//START TEST FOR NEW API

						// Users key empty
						request = new FakeRequest("POST", "/push/message");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileSpecifiedWithoutUsers.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueEmpty request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueEmpty result: " + contentAsString(result));
						assertRoute(result, "error with send, key users empty", Status.BAD_REQUEST, CustomHttpCode.PUSH_NOTFOUND_KEY_USERS.getDescription(), true);

						// Users value different from array
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithValueUsersDifferentFromArray.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, key users invalid", Status.BAD_REQUEST, CustomHttpCode.PUSH_USERS_FORMAT_INVALID.getDescription(), true);

						// Profiles value MUST be Array of String

						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfilesValueExpressedInString.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithUsersValueDifferentFromArray result: " + contentAsString(result));
						assertRoute(result, "error with send, push profiles format invalid(profile expressed on String)", Status.BAD_REQUEST, CustomHttpCode.PUSH_PROFILE_FORMAT_INVALID.getDescription(), true);
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
