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
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.mvc.Http.Status;
import play.test.FakeRequest;
import play.test.TestBrowser;

import core.AbstractAdminTest;
import core.TestConfig;

public class AdminCollectionFunctionalTest extends AbstractAdminTest
{
	//@Override
	public String getRouteAddress()
	{
		return "/admin/collection/" + TestConfig.TEST_COLLECTION_NAME + UUID.randomUUID().toString();
	}
	
	public String getMethod()
	{
		return POST;
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
			fakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					routeCreateCollection();
				}
			}
		);		
	}

	@Test
	public void testServerOK()
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					serverCreateCollection();
	            }
	        }
		);
	}	
	
	public String routeCreateCollection()
	{
		String sFakeCollection = getRouteAddress();

		FakeRequest request = new FakeRequest(getMethod(), sFakeCollection);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		Result result = routeAndCall(request);
		assertRoute(result, "testRouteOK", Status.CREATED, null, false);

		return sFakeCollection.substring(sFakeCollection.lastIndexOf("/") +1);
	}
	
	public String serverCreateCollection()
	{
		String sFakeCollection = getURLAddress();
		
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest(sFakeCollection, getMethod());
		assertServer("testServerOK", Status.CREATED, null, false);
		
		return sFakeCollection.substring(sFakeCollection.lastIndexOf("/") +1);
	}
}
