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
import static play.test.Helpers.running;

import java.util.Date;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.Assert;

import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.user.RoleService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.AbstractDocumentTest;
import core.TestConfig;

public class DocumentAclTest extends AbstractDocumentTest
{
	private static final String TEST_MODIFY_JSON = "\"shape\":\"round\"";
	
	private Object json = null;

	private String sFakeCollection;
	
	@Override
	public String getRouteAddress()
	{
		return SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}
	
	@Override
	protected void assertContent(String s)
	{
		json = toJSON(s);
		assertJSON(json, "@rid");
		assertJSON(json, "id");
		assertJSON(json, "color");
		assertJSON(json, "shape");
	}
	
	//creates a user
	//creates a role
	//creates a new user belonging to the new role
	//create a collection
	
	//@Test
	public void test()	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						
						//setup
						String user1=UUID.randomUUID() + "_acl_user1";
						String user2=UUID.randomUUID() + "_acl_user2";
						String newRole=UUID.randomUUID() + "_acl_user2_role";
						String collection=UUID.randomUUID() + "_acl";
						UserService.signUp(
								user1, 
								user1, 
								new Date(), 
								null, 
								null, 
								null, 
								null, 
								false);
						RoleService.createRole(newRole, "registered", "fake role for ACL test");
						UserService.signUp(
								user2, 
								user2, 
								new Date(), 
								newRole,
								null, 
								null, 
								null, 
								null, 
								false);
						CollectionService.create(collection);
						
						//
						ObjectMapper obm=new ObjectMapper();
						ObjectNode docJson = (ObjectNode)getPayload("/documentCreatePayload.json");
						//ArrayNode allows=obm.readTree("[\"\"]");
						//docJson.put(Permissions.ALLOW_READ, )
						//DocumentService.create(collection, bodyJson)
					} catch (Throwable e) {
						Assert.fail(e.getMessage());
					}
				}
			}
			);
	}
	
	
	private String getRid()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("@rid");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get RID value: " + ex.getMessage());
		}
		
		return sRet;
	}
	
	private String getUuid()
	{
		String sUuid = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		
		return sUuid;
	}
	

	private String getCreationDate()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_creation_date");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get _creation_date value: " + ex.getMessage());
		}
		
		return sRet;
	}
	
	private String getAuthor()
	{
		String sRet = null;

		try
		{
			JSONObject jo = (JSONObject)json;
			sRet = jo.getJSONObject("data").getString("_author");
		}
		catch (Exception ex)
		{
			Assert.fail("Cannot get _author value: " + ex.getMessage());
		}
		
		return sRet;
	}
}
