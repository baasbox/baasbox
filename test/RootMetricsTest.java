/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractRootTest;
import core.TestConfig;



public class RootMetricsTest extends AbstractRootTest{

	@Override
	public String getRouteAddress() {
		return "/root/metrics";
	}

	@Override
	public String getMethod() {
		
		return "POST";
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
	@Test
	public void test()	{
		running	(
			testServer(TestConfig.SERVER_PORT,getFakeApplication()), HTMLUNIT, new Callback<TestBrowser>(){
				public void invoke(TestBrowser browser){
					
					//test metrics
					FakeRequest request5 = new FakeRequest(GET, getRouteAddress() + "/counters");
					request5 = request5.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request5 = request5.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					Result result = routeAndCall(request5);
					assertRoute(result, "MetricsTest.deactivate", 503, "The metrics service are disabled", true);
					
					
					//activate the metrics
					FakeRequest request = new FakeRequest("POST", getRouteAddress());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					 result = routeAndCall(request);
					assertRoute(result, "MetricsTest.start", 200, null, false);
					
					
					//make some calls
					FakeRequest request0 = new FakeRequest(GET, "/admin/user");
					request0 = request0.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request0 = request0.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request0);
					assertRoute(result, "MetricsTest.one", 200, null, false);
					
					FakeRequest request1 = new FakeRequest(GET, "/admin/user");
					request1 = request1.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request1 = request1.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request1);
					assertRoute(result, "MetricsTest.two", 200, null, false);
					
					//test gauges
					FakeRequest request2 = new FakeRequest(GET, getRouteAddress() + "/gauges");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.testGauges1", 200, "filesystem.datafile.spaceleft", true);
					assertRoute(result, "MetricsTest.testGauges2", 200, "filesystem.backupdir.spaceleft", true);
					assertRoute(result, "MetricsTest.testGauges3", 200, "memory.current_allocate", true);
					assertRoute(result, "MetricsTest.testGauges4", 200, "memory.max_allocable", true);
					assertRoute(result, "MetricsTest.testGauges5", 200, "memory.used", true);
					
					//test timers
					request2 = new FakeRequest(GET, getRouteAddress() + "/timers");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.testTimers1", 200, null, false);

					
					//test counters
					request2 = new FakeRequest(GET, getRouteAddress() + "/counters");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.counters", 200, null, false);
					
					//test meters
					request2 = new FakeRequest(GET, getRouteAddress() + "/meters");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.meters", 200, null, false);
					
					//test histograms
					request2 = new FakeRequest(GET, getRouteAddress() + "/histograms");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.histograms", 200, null, false);
					
					//test uptime
					request2 = new FakeRequest(GET, getRouteAddress() + "/uptime");
					request2 = request2.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request2 = request2.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request2);
					assertRoute(result, "MetricsTest.uptime", 200, null, false);
					
					
					//deactivate metrics
					FakeRequest request3 = new FakeRequest("DELETE", getRouteAddress());
					request3 = request3.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request3 = request3.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					result = routeAndCall(request3);
					assertRoute(result, "MetricsTest.stop", 200, null, false);
					
					//test metrics
					FakeRequest request6 = new FakeRequest(GET, getRouteAddress() + "/counters");
					request6 = request6.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request6 = request6.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ROOT_ENC);
					 result = routeAndCall(request5);
					assertRoute(result, "MetricsTest.deactivate2", 503, "The metrics service are disabled", true);
				
				}
			}
		);
	}

}
