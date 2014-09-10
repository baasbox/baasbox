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

import static play.test.Helpers.GET;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import play.mvc.Http.Status;
import play.test.FakeRequest;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.fasterxml.jackson.databind.JsonNode;

import core.AbstractUserTest;
import core.TestConfig;
import play.mvc.Result;

public class UserSlowGetTest_issue_412 extends AbstractUserTest
{
	public static final String USER_TEST = "user_";
	
	@Override
	public String getRouteAddress()
	{
		return ROUTE_USER;
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}
	
	@Override
	protected void assertContent(String s)
	{
		Object json = toJSON(s);
		assertJSON(json, "user");
	}

	@Test
	public void test()	{
		running	(
			getFakeApplication(), 
			new Runnable() 	{
				public void run() 	{
					UUID uuid = UUID.randomUUID();
					Result result=null;
					String sFakeUser =null;
					//create 100 fake users
					for (int i=0;i<100;i++){
						sFakeUser = USER_TEST +uuid + "_" + i;
							
						// Prepare test user
						JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
	
						// Create user
						FakeRequest request = new FakeRequest("POST", "/user");
						request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						request = request.withJsonBody(node, "POST");
						result=routeAndCall(request);
					}//for i
					assertRoute(result, "check username", Status.CREATED, "name\":\""+sFakeUser+"\"", true);
					try {
						DbHelper.open("1234567890", "admin", "admin");
					} catch (InvalidAppCodeException e) {
						//swallow
					}
					Object explain = DbHelper.genericSQLStatementExecute(
							"explain select from _bb_user where user.name = ?", new String[]{USER_TEST +uuid + "_" + 1});
					Assert.assertTrue("UserSlowGetTest_issue_412 FAILED! " + explain.toString(),explain.toString().contains("compositeIndexUsed:1"));
				}//run()
			}// Runnable
		);//running	
	}// test

}
