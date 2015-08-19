import static play.test.Helpers.GET;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.running;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.test.TestBrowser;

import com.baasbox.util.BBJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.AbstractDocumentTest;
import core.TestConfig;


public class DocumentContainsTest extends AbstractDocumentTest {
	private Object json = null;
	private JsonNode document1;
	private JsonNode jsonForUpdate;
	private String sFakeCollection;

	@Override
	public String getRouteAddress() {
		return SERVICE_ROUTE + TestConfig.TEST_COLLECTION_NAME;
	}

	@Override
	public String getMethod() {
		return GET;
	}

	@Override
	protected void assertContent(String s) {
	}
	
	@Before
	public void setDocument() throws JsonProcessingException, IOException{
		document1 = BBJson.mapper().readTree(
				"{\"players\": ["+
					"{"+
						"\"username\": \"XXXX@XXXX.COM\","+
						"\"name\": \"john doe\","+
						"\"points\": 0"+
					"},"+
					"{"+
					"\"username\": \"YYYY1@YYYY.COM\","+
					"\"name\": \"jane doe\","+
					"\"department\": \"one\","+
					"\"points\": 0"+
					"},"+
					"{"+
					"\"username\": \"ZZZZ@ZZZZ1.COM\","+
					"\"name\": \"dana scully\","+
					"\"department\": \"two\","+
					"\"points\": 0"+
					"}"+
				"],"+
				"\"status\": \"open\","+
				"\"player_turn\": 0,"+
				"\"id\": \""+UUID.randomUUID().toString()+"\","+
				"\"creationdate\": \"2014-02-15T13:49:51.051+0100\","+
				"\"_author\": \"admin\""
				+ "}");
	}
	
	@Test
	public void testContains(){
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
			 		continueOnFail(false);
					sFakeCollection = new AdminCollectionFunctionalTest().serverCreateCollection();
				
					//create  document
					serverCreateDocument(sFakeCollection,document1);
					String id1=getUuid();
					
					//read the doc
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection) + "/" + id1, "GET");
					assertServer("testContains load document 1", Status.OK, "\"name\":\"john doe\"", true);	
					
					//select it using the `contains`function
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(sFakeCollection) + "?where=players%20contains%20(username%3D%27XXXX%40XXXX.COM%27)", "GET");
					assertServer("testContains load document 1 using contains. Collection Name: " + sFakeCollection, 200, "\"name\":\"john doe\"", true);					
				}
			}
		);			
	}
	
		private String getUuid()	{
		String sUuid = null;

		try	{
			JsonNode jo = BBJson.mapper().readTree(getResponse());
			sUuid = ((ObjectNode)jo.get("data")).get("id").asText();
		}
		catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
	}
}


