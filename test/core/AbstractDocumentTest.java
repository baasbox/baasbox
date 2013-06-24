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

package core;

import static play.test.Helpers.GET;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.routeAndCall;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;


import play.mvc.Result;
import play.test.FakeRequest;


public abstract class AbstractDocumentTest extends AbstractRouteHeaderTest 
{
	public static final String SERVICE_ROUTE = "/document/";
	public static final String COLLECTION_NOT_EXIST = "fakeCollection";
	
	public String getRouteAddress(String sCollectionName)
	{
		return SERVICE_ROUTE + sCollectionName;
	}

	public String getURLAddress(String sCollectionName)
	{
		return TestConfig.SERVER_URL + getRouteAddress(sCollectionName);
	}
	
	protected Result routeCreateDocument(String sAddress)
	{
	 	FakeRequest request = new FakeRequest(POST, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(getPayload("/documentCreatePayload.json"));
		return routeAndCall(request); 
	}

	public Result routeModifyDocument(String sAddress)
	{
		// Modify created document
		FakeRequest request = new FakeRequest(PUT, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(getPayload("/documentModifyPayload.json"), PUT);
		return routeAndCall(request);
	}
	
	public Result routeGetDocument(String sAddress)
	{
		Result result = null;
		FakeRequest request = new FakeRequest(GET, sAddress);
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		result = routeAndCall(request);
		
		return result;
	}
	
	public void serverCreateDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httpRequest
		( 
			sAddress,
			POST,
			"/documentCreatePayload.json"
		);
	}

	public void serverModifyDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
		httpRequest
		( 
			sAddress,
			PUT,
			"/documentModifyPayload.json"
		);
	}
	
	public void serverGetDocument(String sAddress)
	{
		setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
		httpRequest
		( 
			sAddress,
			GET
		);
	}
}
