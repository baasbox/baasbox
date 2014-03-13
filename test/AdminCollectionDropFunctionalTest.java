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
import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import core.AbstractTest;
import core.TestConfig;

public class AdminCollectionDropFunctionalTest extends AbstractTest{
	

    
	public String collectionName="";
    
	
    
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
            collectionName="fake"+UUID.randomUUID().toString();
            return "/admin/collection/"+ collectionName;
    }
   
    public String getRouteForObjects(){
            return "/document/" + collectionName;
    }
   
    public String getRouteForCount(){
            return "/document/" + collectionName + "/count";
    }
   
   
   
    @Test
    public void testDropCollectionCreate() throws Exception
    {

            running (getFakeApplication(),     
            		new Runnable()  {
                            public void run()       {
                                    try {
                                    	 String sFakeCollection = getRouteAddress();
                                         FakeRequest requestCreation = new FakeRequest(POST, sFakeCollection);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                         requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                         Result result = route(requestCreation);
                                         assertRoute(result, "testDropCollection.create", Status.CREATED, null, true);
                                         
                                         //Insert some object in there
                                         String sFakeInsertObjects = getRouteForObjects();
                                         JsonNode payload = (new ObjectMapper()).readTree("{\"test\":\"testvalue\"}");
                                         int cont=15;
                                         for(int i=0;i<cont;i++){
                                         		 requestCreation	 = new FakeRequest(POST, sFakeInsertObjects);
                                         		 requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                         		 requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                         		 requestCreation = requestCreation.withJsonBody(payload, POST);
                                                 Result result1 = route(requestCreation);
                                                 assertRoute(result1, "testDropCollection.populate", Status.OK, null, true);
                                         }
                                         
                                 		//check if the collection is full
                                 		String sFakeCollectionCount = getRouteForCount();
                                		requestCreation = new FakeRequest(GET, sFakeCollectionCount);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "routeDropCollection.count", Status.OK, "\"count\":"+cont, true);
                                         
                                		//drop the collection
                                		requestCreation = new FakeRequest(DELETE, sFakeCollection);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "routeDropCollection.drop", Status.OK, null, false);
                                		
                                		//check the collection does not exist
                                		requestCreation = new FakeRequest(GET, sFakeCollectionCount);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "routeDropCollection.count_not_found", Status.NOT_FOUND, null, false);
                                         
                                		//try to recreate the same
                                        requestCreation = new FakeRequest(POST, sFakeCollection);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                        requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                        result = route(requestCreation);
                                        assertRoute(result, "testDropCollection.create_the_same", Status.CREATED, null, true);

                                		//check the collection is empty
                                		requestCreation = new FakeRequest(GET, sFakeCollectionCount);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "routeDropCollection.count_must_be_empty", Status.OK, "\"count\":0", true);
                                		
                                		//finally... drop
                                		requestCreation = new FakeRequest(DELETE, sFakeCollection);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                                		requestCreation = requestCreation.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                                		result = route(requestCreation);
                                		assertRoute(result, "routeDropCollection.drop_final", Status.OK, null, false);
                                		
                                		
                                    } catch (JsonProcessingException e) {
                                            fail();
                                    } catch (IOException e) {
                                            fail();
                                    }catch (Exception e) {
                                    		e.printStackTrace();
                                    		fail();
									}
                            }});
    }




	/*
	@Test
	public void testDropCollectionPopulate()
	{
		running	(getFakeApplication(),	new Runnable()	{
				public void run() 	{
					routeDropCollectionPopulate();
				}

				private void routeDropCollectionPopulate() {
					//Insert some object in there
					String sFakeInsertObjects = getRouteForObjects();
					JsonNode payload = getPayload("/documentModifyPayload.json");
					for (int i=0; i<1; i++){
						FakeRequest requestInsertObject = new FakeRequest(POST, sFakeInsertObjects);
						requestInsertObject = requestInsertObject.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						requestInsertObject = requestInsertObject.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						requestInsertObject = requestInsertObject.withJsonBody(payload, POST);
						Result result = route(requestInsertObject);
					}
				}
			}
		);		
	}
	

	@Test
	public void testDropCollectionDrop()
	{
		running	(getFakeApplication(),	new Runnable()	{
				public void run() 	{
					routeDropCollectionPopulate();
				}
			}
		);		
	}
	
	@Test
	public void testDropCollectionCheck()
	{
		running	(getFakeApplication(),	new Runnable()	{
				public void run() 	{
					routeDropCollectionPopulate();
				}
			}
		);		
	}
	
	public String routeDropCollection()
	{
		continueOnFail(false);
		


		/*
		//check if the collection is full
		String sFakeCollectionCount = getRouteForCount();
		FakeRequest requestCreationCount = new FakeRequest(GET, sFakeCollectionCount);
		requestCreationCount = requestCreationCount.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		requestCreationCount = requestCreationCount.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		result = routeAndCall(requestCreationCount);
		assertRoute(result, "routeDropCollection.count", Status.OK, "100", true);

		//drop the collection
		FakeRequest requestDrop = new FakeRequest(DELETE, sFakeCollection);
		requestDrop = requestDrop.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		requestDrop = requestDrop.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		result = routeAndCall(requestDrop);
		assertRoute(result, "routeDropCollection.drop", Status.OK, null, false);

		return sFakeCollection.substring(sFakeCollection.lastIndexOf("/") +1);
	
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
*/
}
