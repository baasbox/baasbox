
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.PUT;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractTest;
import core.TestConfig;

public abstract class PushProfileAbstractTestNotMocked extends AbstractTest {
	private List<String> profiles;

	{
		profiles = new ArrayList<String>();
		profiles.add("profile2");
		profiles.add("profile3");
	}

	protected abstract int getProfile1DisabledReturnCode();
	protected abstract int getProfile1SwitchReturnCode();

	@Test
	public void PushProfileNotMockedOldApi(){
		running
		(
				getFakeApplicationWithDefaultConf(), 
				new Runnable() 	{
					public void run() 	{
						String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();						
						
						//OLD API
						
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						Result result;
						FakeRequest request = new FakeRequest("POST", "/push/message/"+sFakeUser);

						// Send Push, with profiles disabled

						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushProfilesDisabled request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushProfilesDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profiles disabled", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);
						
						// Profile not enabled, with profile specified in Payload
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabledWithProfileSpecified request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled, with profile specified in Payload", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						continueOnFail(true);

						// Profile not enabled, without profile specified in Payload. It should try the profile 1, expected error 503 because it is not enabled
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushPayloadWithoutProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabledWithoutProfileSpecified request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled, without profile specified in Payload", getProfile1DisabledReturnCode(), null, true);

						continueOnFail(true);
						
						//END OLD API
					
					}
				}
				);
	}



	@Test
	public void PushProfileNotMockedNewApi(){
		running
		(
				getFakeApplicationWithDefaultConf(), 
				new Runnable() 	{
					public void run() 	{
						String sFakeUser = new AdminUserFunctionalTest().routeCreateNewUser();						
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						
						
						//NEW API
								
						// Send Push, with profiles disabled
						FakeRequest request = new FakeRequest("POST", "/push/message");
						
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushNewApiPayloadWithProfileSpecified.json"), play.test.Helpers.POST);
						Result result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushProfilesDisabled request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushProfilesDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profiles disabled, without profile specified in Payload", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						// Profile not enabled, with profile specified in Payload
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
						request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
						request = request.withJsonBody(getPayload("/pushNewApiPayloadWithProfileSpecified.json"), play.test.Helpers.POST);
						result = routeAndCall(request);
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabledWithProfileSpecified request: " + request.getWrappedRequest().headers());
						if (Logger.isDebugEnabled()) Logger.debug("sendPushWithProfileDisabled result: " + contentAsString(result));
						assertRoute(result, "error with send, push profile disabled, with profile specified in Payload", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_PROFILE_DISABLED.getDescription(), true);

						continueOnFail(true);


					

					}
				}
				);
	}


	/***
	 * test the switch between sandbox and production mode
	 */
	@Test
	public void PushProfileNotMockedSwitchMode(){
		running
		(
				getFakeApplicationWithDefaultConf(), 
				new Runnable() 	{	
					public void run() 	{
						FakeRequest request;
						Result result;
						String sAuthEnc = TestConfig.AUTH_ADMIN_ENC;
						//Profile is disabled, so it's possible to switch mode
						for(String listprofile : profiles){
							request = new FakeRequest("PUT", "/admin/configuration/Push/"+listprofile+".push.sandbox.enable");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
							request = request.withJsonBody(getPayload("/pushDisableSandbox.json"), getMethod());
							result = routeAndCall(request);
							if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode request: " + request.getWrappedRequest().headers());
							if (Logger.isDebugEnabled()) Logger.debug("disablePushSandboxMode result: " + contentAsString(result));
							assertRoute(result, "switch sandbox for ("+listprofile+") disabled ", Status.OK, null, true);	
						}
						
						//Enable profile which is disabled but configuration missing
						for(String profile : profiles){
							request = new FakeRequest("PUT", "/admin/configuration/Push/"+profile+".push.profile.enable");
							request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
							request = request.withHeader(TestConfig.KEY_AUTH, sAuthEnc);
							request = request.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
							request = request.withJsonBody(getPayload(getDefaultPayload()), getMethod());
							result = routeAndCall(request);
							if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile request: " + request.getWrappedRequest().headers());
							if (Logger.isDebugEnabled()) Logger.debug("enablePushProfile result: " + contentAsString(result));
							assertRoute(result, "configuration missing for the selected profile ("+profile+")", Status.SERVICE_UNAVAILABLE, CustomHttpCode.PUSH_CONFIG_INVALID.getDescription(), true);
						}
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
