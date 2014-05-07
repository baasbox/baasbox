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

import static play.mvc.Http.Status.OK;
import static play.test.Helpers.GET;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.configuration.Application;
import com.baasbox.db.DbHelper;

import core.AbstractAdminTest;
import core.TestConfig;

public class OverrideSettingsTest extends AbstractAdminTest
{
	@Override
	public String getRouteAddress()
	{
		return "/admin/configuration/dump.json";
	}

	@Override
	public String getMethod()
	{
		return GET;
	}

	
	
	@Test 
	public void test()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					//load settings
					FakeRequest request = new FakeRequest(GET, getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON 1", OK, "application.name\":\"BaasBox\",\"description\":\"The App name\",\"type\":\"String\",\"editable\":true,\"visible\":true,\"overridden\":false", true);
					
					//override setting
					Application.APPLICATION_NAME.override("blablabla");
					
					//reload settings
					request = new FakeRequest(GET, getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON 2", OK, 
							"application.name\":\"blablabla\",\"description\":\"The App name\",\"type\":\"String\",\"editable\":false,\"visible\":true,\"overridden\":true", true);
				
					//tries to edit the value
					try{
						Application.APPLICATION_NAME.setValue("baasbox");
						Assert.fail("APPLICATION_NAME changed, but it is overridden");
					}catch (java.lang.IllegalStateException e){
					}catch (Throwable t){
						Assert.fail(t.getMessage());
					}

					Application.APPLICATION_NAME.setVisible(false);
					//reload settings
					request = new FakeRequest(GET, getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON 3", OK, 
							"application.name\":\"--HIDDEN--\",\"description\":\"The App name\",\"type\":\"String\",\"editable\":false,\"visible\":false,\"overridden\":true", true);
				
					//get the hidden value
					try{
						DbHelper.open("1234567890", "admin", "admin");
						String hiddenValue=(String)Application.APPLICATION_NAME.getValue();
						Assert.assertTrue("application.name: expected 'blablabla', it is " + hiddenValue, hiddenValue.equals("blablabla"));
					}catch (Exception e){
						Assert.fail(e.getMessage());
					}finally{
						DbHelper.close(DbHelper.getConnection());
					}
					
					//get the real-not-overridden value
					try{
						DbHelper.open("1234567890", "admin", "admin");
						String hiddenValue=(String)Application.APPLICATION_NAME._getValue();
						Assert.assertTrue("application.name: expected 'BaasBox', it is " + hiddenValue, hiddenValue.equals("BaasBox"));
						
					}catch (Exception e){
						Assert.fail(e.getMessage());
					}finally{
						DbHelper.close(DbHelper.getConnection());
					}
					
					//reset the original values
					Application.APPLICATION_NAME.setVisible(true);
					Application.APPLICATION_NAME.override("BaasBox");
					Application.APPLICATION_NAME.setEditable(true);
					Application.APPLICATION_NAME._setOverridden(false);
				}
			}
		);		
	}

	

	
	
	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
}
