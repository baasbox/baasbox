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


import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;

public class RoleFunctionalTest extends AbstractTest{
	

    
	public String roleName="";
	public String userName="";
	
    
    @Override
	public String getMethod() {
		return GET;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}

	@Override
    public String getRouteAddress()
    {
            roleName="fake"+UUID.randomUUID().toString();
            return "/admin/role/"+ roleName;
    }
   

	public String getFakeUserCreationAddress(){
        userName="fake"+UUID.randomUUID().toString();
        return "/admin/user";
	}
   
	
	public String getFakeUserAddress(){
		return "/admin/user/"+ userName;
	}
   
	
    @Test
    public void testRoleCreate() throws Exception
    {

            running (getFakeApplication(),     
            		new Runnable()  {
                            public void run()       {
                                    try {
                                    	 String sFakeRole = getRouteAddress();
                                         FakeRequest requestCreation = new FakeRequest(POST, sFakeRole);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                         Result result = route(requestCreation);
                                         assertRoute(result, "testRoleCreate.create", Status.CREATED, null, true);
                                         
                                        //creates one user in this Role
                                        String sFakeCreateUser = getFakeUserCreationAddress();
                                        requestCreation = new FakeRequest(POST, sFakeCreateUser);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                        ObjectMapper mapper = new ObjectMapper();
                                        JsonNode actualObj = mapper.readTree("{\"username\":\""+userName+"\","
                                        		+ "\"password\":\"test\","	
                                        		+ "\"role\":\""+ roleName +"\"}");
                                        requestCreation = requestCreation.withJsonBody(actualObj);
                                        requestCreation = requestCreation.withHeader("Content-Type", "application/json");
                                        result = route(requestCreation);
                                        assertRoute(result, "testRoleCreate.createUser", Status.CREATED, null, true);
                                       

                                 		//checks the user
                                 		String sFakeCheckUser = getFakeUserAddress();
                                		requestCreation = new FakeRequest(GET, sFakeCheckUser);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.checkUser", Status.OK, "\"name\":\""+roleName+"\"", true);
                                        
                                		                                       
                                		//drops the role
                                		requestCreation = new FakeRequest(DELETE, sFakeRole);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.drop", Status.OK, null, false);
                                		
                                		//checks that the role does not exist
                                		requestCreation = new FakeRequest(GET, sFakeRole);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.role_not_found", Status.NOT_FOUND, null, false);
                                         
                                		//checks that the fake user belongs to the registered role
                                 		sFakeCheckUser = getFakeUserAddress();
                                		requestCreation = new FakeRequest(GET, sFakeCheckUser);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.checkUser", Status.OK, "\"name\":\"registered\"", true);
                                		
                                		
                                		//tries to recreate the same role, now with description
                                        requestCreation = new FakeRequest(POST, sFakeRole);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                        mapper = new ObjectMapper();
                                        actualObj = mapper.readTree("{\"description\":\"this is a test\"}");	
                                        requestCreation = requestCreation.withJsonBody(actualObj);
                                        requestCreation = requestCreation.withHeader("Content-Type", "application/json");
                                        result = route(requestCreation);
                                        assertRoute(result, "testRoleCreate.create_the_same", Status.CREATED, null, true);
 
                                		//checks the role
                                		requestCreation = new FakeRequest(GET, sFakeRole);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.check_with_desc", Status.OK, "\"description\":\"this is a test\"", true);
                                                                     		
                                		//updates the role name and description
                    					
                                		 requestCreation = new FakeRequest(PUT, sFakeRole);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                         mapper = new ObjectMapper();
                                         actualObj = mapper.readTree("{\"description\":\"this is new test\"}");	
                                         requestCreation = requestCreation.withJsonBody(actualObj,PUT);
                                         result = route(requestCreation);
                                         assertRoute(result, "testRoleCreate.update_desc", Status.OK, null, true);
                                         
                                                                         		
                                		//checks the role
                                         requestCreation = new FakeRequest(GET, sFakeRole);
                                 		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                 		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                 		result = route(requestCreation);
                                 		assertRoute(result, "testRoleCreate.check_with_new_desc", Status.OK, "\"description\":\"this is new test\"", true);
                                       

                                		//
                                		//finally... drop it, again
                                		requestCreation = new FakeRequest(DELETE, sFakeRole);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.drop_final", Status.OK, null, false);
                                 		
                                		
                                    }catch (Exception e) {
                                    		e.printStackTrace();
                                    		fail();
									}
                            }});
    }



}
