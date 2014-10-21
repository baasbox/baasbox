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
// Note: The AbstractTest is not thread safe. It's thinked to run only as JUnit test.

package core;

import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.status;
import static play.test.Helpers.testServer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.fluentlenium.adapter.FluentTest;
import org.hamcrest.CoreMatchers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import play.Configuration;
import play.Logger;
import play.Play;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;
import play.test.TestServer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class AbstractTest extends FluentTest
{
	public static final String PARAM_FILE = "file";
	private static final String BOUNDARY = "*****";
	
	@Rule
	public ErrorCollector collector = new ErrorCollector();
	
	protected HtmlUnitDriver webDriver = new HtmlUnitDriver();
	private static Map<String, JsonNode> mPayloadsCache = new Hashtable<String, JsonNode>();
	private static Map<String, byte[]> mResourcesCache = new Hashtable<String, byte[]>();
	private static Map<String, String> mHeaders = new HashMap<String, String>();
	private BasicNameValuePair nvpFile = null;
	private String sResponse = null;
	private int nStatusCode = -1;
	private boolean fUseCollector = false;

	public String createNewUser(String username) {
		String sFakeUser = username + UUID.randomUUID();
		// Prepare test user
		JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);

		// Create user
		FakeRequest request = new FakeRequest(POST, "/user");
		request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
		request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
		request = request.withJsonBody(node, POST);
		Result result = routeAndCall(request);
		assertRoute(result, "Create user.", Status.CREATED, null, false);
		return sFakeUser;
	}
	
	protected static void resetHeaders(){
		mHeaders.clear();
	}
	protected static FakeApplication getFakeApplication(){
		return fakeApplication(additionalConfigurations.asMap());
	}
	
	protected static FakeApplication getFakeApplicationWithDefaultConf(){
		return fakeApplication();
	}
	
	protected static TestServer getTestServer(){
		return testServer(TestConfig.SERVER_PORT,getFakeApplication());
	}
	
	protected static TestServer getTestServerWithDefaultConf(){
		return testServer(TestConfig.SERVER_PORT,getFakeApplicationWithDefaultConf());
	}
	
	protected  static Configuration additionalConfigurations=null;
	static{
	    Config additionalConfig = ConfigFactory.parseFile(new File("conf/rootTest.conf"));
	    additionalConfigurations = new Configuration(additionalConfig);
	}


	// Abstract methods
	public abstract String getRouteAddress();
	public abstract String getMethod();
	protected abstract void assertContent(String s);
	
	@Before
	public void resetAllHeadersBeforeTests(){
		resetHeaders();
	}
	
	@Override
    public WebDriver getDefaultDriver() 
	{
        return webDriver;
    }
	
	public String getDefaultPayload()
	{
		return null;
	}
	
	protected String getURLAddress()
	{
		return TestConfig.SERVER_URL + getRouteAddress();		
	}

	protected void setHeader(String sName, String sValue)
	{
		mHeaders.put(sName, sValue);
	}

	protected void setMultipartFormData()
	{
		setHeader(HTTP.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA + ";boundary=" + BOUNDARY);
	}
	
	protected void setAssetFile(String sFile, String sEncoding)
	{
		nvpFile = new BasicNameValuePair(sFile, sEncoding);
	}
	
	protected void setFile(String sFile, String sEncoding)
	{
		nvpFile = new BasicNameValuePair(sFile, sEncoding);
	}
	
	protected void removeHeader(String sName)
	{
		mHeaders.remove(sName);
	}
	
	protected void removeAllHeaders()
	{
		mHeaders.clear();
	}
	
	protected int getStatusCode()
	{
		return nStatusCode;
	}

	protected String getResponse()
	{
		return sResponse;
	}
	
	private void setResponse(String s)
	{
		sResponse = s;
	}
	
	private void setStatusCode(int n)
	{
		nStatusCode = n;
	}

	protected int httpRequest(String sUrl, String sMethod)
	{
		return httpRequest(sUrl, sMethod, null, null);
	}
	
	protected int httpRequest(String sUrl, String sMethod, Map<String, String> mParameters)
	{
		return httpRequest(sUrl, sMethod, null, mParameters);
	}
	
	protected int httpRequest(String sUrl, String sMethod, String sPayload)
	{
		JsonNode node = null;
		if (sPayload != null)
		{
			node = getPayload(sPayload);
		}
		return httpRequest(sUrl, sMethod, node, null);
	}

	protected int httpRequest(String sUrl, String sMethod, JsonNode payload)
	{
		return httpRequest(sUrl, sMethod, payload, null);
	}
	
	private int httpRequest(String sUrl, String sMethod, JsonNode payload, Map<String, String> mParameters)
	{
		Logger.info("\n\nREQUEST:\n"+sMethod+ " " + sUrl+"\nHEADERS: " + mHeaders+"\nParameters: " +mParameters + "\nPayload: " + payload+"\n");
		HttpURLConnection conn = null;
        BufferedReader br = null;
	    int nRet = 0;
	    boolean fIsMultipart = false;
	    
	    try 
	    {
	    	setStatusCode(-1);
	    	setResponse(null);
	    	conn = getHttpConnection(sUrl, sMethod);
	    	if (mHeaders.size() > 0)
	    	{
	    		Set<String> keys = mHeaders.keySet();
	    		for(String sKey: keys)
	    		{
	    			conn.addRequestProperty(sKey, mHeaders.get(sKey));
	    			if (sKey.equals(HTTP.CONTENT_TYPE))
	    			{
	    				if (mHeaders.get(sKey).startsWith(MediaType.MULTIPART_FORM_DATA))
	    				{
	    					fIsMultipart = true;
	    				}
	    			}
	    		}
	    	}

	    	if (payload != null || mParameters != null)
	    	{
				DataOutputStream out = new DataOutputStream(conn.getOutputStream());
				
				try
				{
			    	if(payload != null)
			    	{
			    		//conn.setRequestProperty("Content-Length", "" + node.toString().length());
						out.writeBytes(payload.toString());
		            }
			    	
			    	if (mParameters != null)
			    	{
			    		Set<String> sKeys = mParameters.keySet();
			    		
			    		if (fIsMultipart)
			    		{
		    				out.writeBytes("--" + BOUNDARY + "\r\n");
			    		}
			    		
			    		for (String sKey: sKeys)
			    		{
			    			if (fIsMultipart)
			    			{
		    					out.writeBytes("Content-Disposition: form-data; name=\"" + sKey + "\"\r\n\r\n");    
		    					out.writeBytes(mParameters.get(sKey));
		    					out.writeBytes("\r\n");
			    				out.writeBytes("--" + BOUNDARY + "--\r\n");
			    			}	
			    			else
			    			{
				    			out.writeBytes(URLEncoder.encode(sKey, "UTF-8"));
				    			out.writeBytes("=");
				    			out.writeBytes(URLEncoder.encode(mParameters.get(sKey), "UTF-8"));
				    			out.writeBytes("&");
			    			}
			    		}
			    		
			    		if (fIsMultipart)
			    		{
			    			if (nvpFile != null)
			    			{
				    			File f = Play.application().getFile(nvpFile.getName());
				    			if (f == null)
				    			{
				    				assertFail("Cannot find file <" + nvpFile.getName() + ">");
				    			}
				    			FileBody fb = new FileBody(f);
			    				out.writeBytes("Content-Disposition: form-data; name=\"" + PARAM_FILE + "\";filename=\"" + fb.getFilename() + "\"\r\n");
			    				out.writeBytes("Content-Type: " + nvpFile.getValue() + "\r\n\r\n");
			    				out.write(getResource(nvpFile.getName()));
			    			}
			    			out.writeBytes("\r\n--" + BOUNDARY + "--\r\n");
			    		}
			    	}
				}
				catch (Exception ex)
				{
					assertFail("Send request: " + ex.getMessage());
				}
				finally
				{
					try
					{
						out.flush();
					}
					catch (Exception ex) {}
					try
					{
						out.close();
					}
					catch (Exception ex) {}
				}
	    	}
	    
	    	nRet = conn.getResponseCode();
	    	setStatusCode(nRet);
	    	if (nRet / 100 != 2)
	    	{
	    		if (conn.getErrorStream() != null)
	    		{
	    			br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	    		}
	    	}
	    	else
	    	{
	    		if (conn.getInputStream() != null)
	    		{
	    			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	    		}
	    	}
	    	
	    	if (br != null)
	    	{
		        String temp = null;
		        StringBuilder sb = new StringBuilder(1024);
		        while((temp = br.readLine()) != null)
		        {
		            sb.append(temp).append("\n");
		        }
		        setResponse(sb.toString().trim());
	    	}
	    	Logger.info("\nRESPONSE\nHTTP code: "+nRet+"\nContent: " + sResponse + "\n");
	    } 
	    catch (Exception ex) 
	    {
	    	assertFail("httpRequest: " + ex.getMessage());
	    }
	    finally
	    {
	    	if (br != null)
	    	{
	    		try
	    		{
	    			br.close();
	    		}
	    		catch (Exception ex) {}
	    	}
	    	if (conn != null)
	    	{
	    		conn.disconnect();
	    	}
	    }
	    
	    return nRet;
	}

	private HttpURLConnection getHttpConnection(String sUrl, String sMethod)
	{
        URL uri = null;
        HttpURLConnection conn = null;

        try
        {
            uri = new URL(sUrl);
            conn = (HttpURLConnection)uri.openConnection();
            conn.setDoInput(true);
			conn.setDoOutput(true);
            conn.setRequestMethod(sMethod); // POST, PUT, DELETE, GET
            conn.setUseCaches(false);
			conn.setRequestProperty("Cache-Control", "no-cache");
			conn.setRequestProperty("Connection", "Keep-Alive");			
            //conn.setConnectTimeout(60000); //60 secs
            //conn.setReadTimeout(60000); //60 secs
            //conn.setRequestProperty("Accept-Encoding", "UTF-8");
        }
        catch(Exception e)
        {
            Assert.fail("Unable to get HttpConnection "+e.getMessage());
        }

        return conn;
	}	
	
	protected JsonNode getPayload(String sName)
	{
		InputStream is = null;
		JsonNode node = null;

		//if ((node = mPayloadsCache.get(sName)) == null)
		//{
			try
			{
				is = Play.application().resourceAsStream(sName);
				ObjectMapper om = new ObjectMapper();
				node = om.readTree(is);
			}
			catch (Exception ex)
			{
				Assert.fail("Cannot process payload <" + sName + ">. Due to: " + ex.getMessage());
			}
			finally
			{
				if (is != null)
				{
					try
					{
						is.close();
					}
					catch (Exception ex) {}
				}
			}
		//	mPayloadsCache.put(sName, node);
		//}
		
		return node;
	}

	protected String getPayloadFieldValue(String sPayload, String sFieldName)
	{
		JsonNode node = getPayload(sPayload);
		return node.get(sFieldName).asText();
	}
	
	protected JsonNode updatePayloadFieldValue(String sPayload, String sFieldName, String sValue)
	{
		JsonNode node = getPayload(sPayload);
		((ObjectNode)node).put(sFieldName, sValue);

		return node;
	}
	
	protected JsonNode updatePayloadFieldValue(String sPayload, String sFieldName, String[] values)
	{
		JsonNode node = getPayload(sPayload);
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode array = mapper.valueToTree(Arrays.asList(values));
		((ObjectNode)node).putArray(sFieldName).addAll(array);
		return node;
	}
	
	protected byte[] getResource(String sName)
	{
		InputStream is = null;
		byte[] abRet = null;

		if ((abRet = mResourcesCache.get(sName)) == null)
		{
			try
			{
				is = Play.application().resourceAsStream(sName);
				abRet = new byte[is.available()];
				is.read(abRet);
				if (is.available() > 0)
				{
					assertFail("Resource too big <" + sName + ">");
				}
			}
			catch (Exception ex)
			{
				assertFail("Cannot read resource <" + sName + ">. Due to: " + ex.getMessage());
			}
			finally
			{
				if (is != null)
				{
					try
					{
						is.close();
					}
					catch (Exception ex) {}
				}
			}
			mResourcesCache.put(sName, abRet);
		}
		
		return abRet;
	}
	
	protected void continueOnFail(boolean f)
	{
		fUseCollector = f;
	}
	
	protected void assertFail(String sMessage)
	{
		if (fUseCollector)
		{
			collector.addError(new Exception(sMessage));
		}
		else
		{
			Assert.fail(sMessage);
		}
	}
	
	protected void assertRoute(Result result, String sTestName, int nExptedStatus, String sExpctedContent, boolean fCheckContent)
	{
		sTestName= "Test name: " + sTestName+"  -  ";
		if (fUseCollector)
		{
			if (result == null)
			{
				collector.addError(new Exception(sTestName + ". Cannot route to specified address."));
			}
			else if (status(result) !=  nExptedStatus)
			{
				collector.addError(new Exception(sTestName + ". Http status code: Expected is: " + nExptedStatus + " got: " + status(result) + " Response <" + contentAsString(result) + ">"));
			}
		}
		else
		{
			Assert.assertNotNull(sTestName + ". Cannot route to specified address.", result);
			Assert.assertEquals(sTestName + ". Http status code. Response <" + contentAsString(result) + ">", nExptedStatus, status(result));
		}

		if (fCheckContent && result != null)
		{
			String sContent = contentAsString(result);
			if (sExpctedContent != null)
			{
				if (fUseCollector)
				{
					if (!sContent.contains(sExpctedContent))
					{
						collector.addError(new Exception(sTestName + ". Unexpected content <" + sContent + "> was expcted <" + sExpctedContent + ">"));
					}
				}
				else
				{
					Assert.assertTrue(sTestName + ". Unexpected content <" + sContent + "> was expected <" + sExpctedContent + ">", sContent.contains(sExpctedContent));
				}
			}
			else
			{
				assertContent(sContent);
			}
		}
	}
	
	protected void assertServer(String sTestName, int nExptedStatus, String sExpctedContent, boolean fCheckContent)
	{
		sTestName="Test name: " +sTestName+"  -  ";
		if (fUseCollector)
		{
			if (getStatusCode() != nExptedStatus)
			{
				collector.addError(new Exception(sTestName + ". Http status code: Expected was: " + nExptedStatus + " got: " + getStatusCode() + " Response <" + getResponse() + ">"));
			}
		}
		else
		{
			Assert.assertEquals(sTestName + ". Http status code. Response <" + getResponse() + ">", nExptedStatus, getStatusCode());
		}
		
		if (fCheckContent)
		{
			String sContent = getResponse();
			if (sExpctedContent != null)
			{
				if (fUseCollector)
				{
					if (!sContent.contains(sExpctedContent))
					{
						collector.addError(new Exception(sTestName + ". Unexpected content <" + sContent + "> was expected <" + sExpctedContent + ">"));
					}
				}
				else
				{
					Assert.assertTrue(sTestName + ". Unexpected content <" + sContent + "> was expected <"+sExpctedContent+">", sContent.contains(sExpctedContent));
				}
			}
			else
			{
				assertContent(sContent);
			}
		}
	}
	
	
	protected String getUuid(Result result)
	{
		return getUuid(contentAsString(result));
	}
	
	protected String getUuid(String content)
	{
		String sUuid = null;
		Object json = toJSON(content);
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
	
	protected Object toJSON(String s)
	{
		Object objRet = null;
		
		try
		{
			if (s.startsWith("["))
			{
				objRet = new JSONArray(new JSONTokener(s));
			}
			else if (s.startsWith("{"))
			{
				objRet = new JSONObject(new JSONTokener(s));
			}
			else
			{
				assertFail("Unexpeted JSON object <" + s + ">");
			}
		}
		catch (Exception ex)
		{
			assertFail("Exception parsing JSON: " + ex);
		}
		
		return objRet;
	}
	
	protected void assertJSONString(Object jo, String sToFind)
	{
		String s = null;
		
		if (jo instanceof JSONArray)
		{
			s = ((JSONArray)jo).toString();
		}
		else if (jo instanceof JSONObject)
		{
			s = ((JSONObject)jo).toString();
		}
		
		if (s != null)
		{
			if (fUseCollector)
			{
				if (!s.contains(sToFind))
				{
					collector.addError(new Exception("Element not found <" + sToFind + "> in JSON object <" + s + ">"));
				}
			}
			else
			{
				Assert.assertTrue("Element not found <" + sToFind + "> in JSON object <" + s + ">", s.contains(sToFind));
			}
		}
		else
		{
			assertFail("Unexcpted JSON result <" + s + ">");
		}
	}
	
	protected void assertJSON(Object json, String sKey)
	{
		if (json instanceof JSONArray)
		{
			assertJSONElement((JSONArray)json, sKey);
		}
		else if (json instanceof JSONObject)
		{
			try{

				Object data = ((JSONObject)json).get("data");
				assertJSON(data, sKey);
			}catch (JSONException e){
				assertJSONElement((JSONObject)json, sKey);
			}
			
		}
	}
	
	private void assertJSONElement(JSONArray ja, String sKey)
	{
		try
		{
			JSONObject jso = ja.getJSONObject(0);
			if (fUseCollector)
			{
				collector.checkThat(jso, CoreMatchers.notNullValue());
			}
			else
			{
				Assert.assertNotNull("Empty JSON array.", jso);
			}
			assertJSONElement(jso, sKey);
		}
		catch (Exception ex)
		{
			Assert.fail("(assertJSONElement - JSONArray)  Error: " + ex.getMessage()+ ". Unexpected content: object " + sKey + " not found in JSON: " + ja.toString());
		}
	}

	private void assertJSONElement(JSONObject jo, String sKey)
	{
		try
		{
			JSONObject data = jo;
			Object obj = data.get(sKey);
			if (fUseCollector)
			{
				if (obj == null)
				{
					collector.addError(new Exception("Missed info: " + sKey + " in JSON object <" + jo.toString() + ">"));
				}
			}
			else
			{
				Assert.assertNotNull("Missed info: " + sKey + " in JSON object <" + jo.toString() + ">", obj);
			} 
		}
		catch (Exception ex)
		{
			assertFail("(assertJSONElement - JSONObject) Error: " + ex.getMessage()+ ". Unexpected content: object " + sKey + " not found in JSON: " + jo.toString());
		}
	}
}
