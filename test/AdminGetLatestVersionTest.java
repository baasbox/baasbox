/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

// @author: Marco Tibuzzi

import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;

import com.baasbox.service.logging.BaasBoxLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;

import core.AbstractAdminTest;
import core.TestConfig;

public class AdminGetLatestVersionTest extends AbstractAdminTest
{
	//@Override
	public String getRouteAddress()
	{
		return "/admin/version/latest";
	}
	
	public String getMethod()
	{
		return "GET";
	}
	
	//@Override
	protected void assertContent(String sContent)
	{
	}
	
	@Test
	public void testRouteOK()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					getLatestVersion();
				}
			}
		);		
	}

	@Test
	public void testServerOK()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					getLatestVersion();
	            }
	        }
		);
	}	
	
			
	
	public void getLatestVersion()
	{
		FakeRequest request = new FakeRequest(getMethod(), getRouteAddress());
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		String response=contentAsString(result);
		Assert.assertEquals("The UUID of this installation has not been generated", false, response.contains("bbid=null"));
	}
	

}
