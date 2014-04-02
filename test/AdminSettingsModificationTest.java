import static org.junit.Assert.*;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

import java.util.Iterator;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;
import play.test.Helpers;
import core.AbstractAdminTest;
import core.TestConfig;


public class AdminSettingsModificationTest extends AbstractAdminTest{

	String originalValue;
	
	@Override
	public String getRouteAddress() {
		return "/admin/configuration/Application/application.name/fromquerystring";
	}
	
	public String getRouteAddressWithoutQS() {
		return "/admin/configuration/Application";
	}

	@Override
	public String getMethod() {
		return PUT;
	}

	@Before
	public void getOriginalValue(){
		if(StringUtils.isEmpty(originalValue)){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					JsonFactory factory = new JsonFactory();
					ObjectMapper mp = new ObjectMapper(factory);
					
					//Verify value has changed
					FakeRequest request = new FakeRequest("GET", "/admin/configuration/dump.json");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON", OK, 
							"application.name\":\"fromquerystring\"", true);
				
					String configurationSettings = Helpers.contentAsString(result);
					
					JsonNode configurationAsJson = null;
					try{
						configurationAsJson = mp.readTree(configurationSettings);
					}catch(Exception e){
						fail(e.getMessage());
					}
					 originalValue = findInConfigurationDump(configurationAsJson,"Application","application","application.name");
				}});
		}
	}
	
	/*@After
	public void setOriginalValue(){
		if(!StringUtils.isEmpty(originalValue)){
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					setHeader("Content-Type", "application/json");
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					int status = httpRequest(getURLAddress(), PUT,"{\"key\":\"application.name\",\"value\":\""+originalValue+"\"}");
					Assert.assertEquals(status,200);
				}});
		}
	}*/
	
	
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
					Assert.assertNotNull("Original application.name value not found", originalValue);
					
					FakeRequest request = new FakeRequest(PUT, getRouteAddressWithoutQS());
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					//Commenting next line invokes the controller
					request = request.withJsonBody(getPayload("/keyValueSettingsUpdate.json"));
					
					Result result = routeAndCall(request);
					assertRoute(result,"Test update settings via json body",OK,null,false);
					
					request = new FakeRequest("GET", "/admin/configuration/dump.json");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					assertRoute(result, "LoadConfigurationAsJSON", OK, 
							"application.name\":\"frombody\"", true);
				
					
					
				}

				
			}
		);		
	}
	
	private String findInConfigurationDump(JsonNode data,
			String section,String subsection, String key) {
		Iterator<JsonNode> values = data.get("data").getElements();
		String result = null;
		while(values.hasNext()){
			JsonNode n = values.next();
			
			if(n.has("section") && n.get("section").getTextValue().equalsIgnoreCase(section)){
				JsonNode subValues = n.get("sub sections").get(subsection);
				
				if(subValues!= null){
					Iterator<JsonNode> keys = subValues.getElements();
					while(keys.hasNext()){
						JsonNode keyNode = keys.next();
						if(keyNode.has(key)){
							result = keyNode.get(key).getTextValue();
							break;
						}
					}
				}
			}
			
		}
		return result;
		
	}
	
	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
	

}
