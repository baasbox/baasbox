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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;

public class RoleInheritsFunctionalTest extends AbstractTest{
	

    
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
                                		//admin creates a document
                                		String sFakeCollection = new AdminCollectionFunctionalTest().routeCreateCollection();		
                                		result = routeCreateDocumentAsAdmin("/document/"+sFakeCollection);
                     					assertRoute(result, "adminCreateDocument", Status.OK, null, true);
                     					String sUUID = getUuid(result);
                                		
                                		//and gives to the registered user the read grant
                     					requestCreation = new FakeRequest(PUT, "/document/"+sFakeCollection + "/" + sUUID + "/read/role/registered");
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.AdminGivesGrant", Status.OK, null, false);
                                		
                                		//now the new user should see it
                                		requestCreation = new FakeRequest(GET, "/document/"+sFakeCollection + "/" + sUUID);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(userName, "test"));
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.UserShouldRead", Status.OK, "id\":\""+sUUID, true);
                                		
                                		//registered user should see it as well
                                		 String sFakeRegUser = "regUser_"+UUID.randomUUID();
                                         requestCreation = new FakeRequest(POST, "/admin/user");
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                         mapper = new ObjectMapper();
                                         actualObj = mapper.readTree("{\"username\":\""+sFakeRegUser+"\","
                                         		+ "\"password\":\"test\","	
                                         		+ "\"role\":\"registered\",\"isrole\":true}");
                                         requestCreation = requestCreation.withJsonBody(actualObj);
                                         requestCreation = requestCreation.withHeader("Content-Type", "application/json");
                                         result = route(requestCreation);
                                         assertRoute(result, "testRoleCreate.createRegUser", Status.CREATED, null, true);
                                		
                                         requestCreation = new FakeRequest(GET, "/document/"+sFakeCollection + "/" + sUUID);
                                 		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                 		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeRegUser, "test"));
                                 		result = route(requestCreation);
                                 		assertRoute(result, "testRoleCreate.RegShouldRead", Status.OK, "id\":\""+sUUID, true);
                                 		
                                		//user create a document and gives grant to its role
                                 		result = routeCreateDocumentAsUser(sFakeCollection,userName,"test");
                     					assertRoute(result, "testRoleCreate.userCreateDoc", Status.OK,null, false);
                     					sUUID = getUuid(result);
                     					
                     					requestCreation = new FakeRequest(PUT, "/document/"+sFakeCollection + "/" + sUUID + "/read/role/"+roleName);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(userName, "test"));
                                		result = route(requestCreation);
                                		assertRoute(result, "testRoleCreate.UserGivesGrant", Status.OK, null, false);
                                		
                                		//registered user should not see it
                                		 requestCreation = new FakeRequest(GET, "/document/"+sFakeCollection + "/" + sUUID);
                                  		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                  		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(sFakeRegUser, "test"));
                                  		result = route(requestCreation);
                                  		assertRoute(result, "testRoleCreate.RegShouldNotSeeRead", Status.NOT_FOUND, null, false);	
                                		
                                		//
                                		//finally... drop the new role
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

	protected Result routeCreateDocumentAsAdmin(String sAddress)
	{
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
		return routeAndCall(request); 
	}

	protected Result routeCreateDocumentAsUser(String collection, String username,String password)
	{
	 	FakeRequest request = new FakeRequest(POST, "/document/" + collection);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(username, password));
		request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
		return routeAndCall(request); 
	}
	
}
