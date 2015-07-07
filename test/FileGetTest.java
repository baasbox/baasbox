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
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import play.mvc.SimpleResult;
import play.core.j.JavaResultExtractor;
import play.libs.F.Callback;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;
import core.AbstractFileTest;
import core.TestConfig;


public class FileGetTest extends AbstractFileTest{
	
	private Object json = null;
	
	private String sTestName="FileGetTest";
	
	@Override
	public String getRouteAddress()
	{
		return "/file/details";
	}
	
	public String getPostServerRouteAddress()
	{
		return TestConfig.SERVER_URL +"/file";
	}
	
	public String getStreamFileRouteAddress()
	{
		return "/file";
	}

	public String getAttachedDataFileRouteAddress()
	{
		return "/file/attachedData";
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}


	@Override
	protected void assertContent(String s)	{
		json = toJSON(s);
	}
	
	private String getUuid(){
		String sUuid = null;
		try	{
			JSONObject jo = (JSONObject)json;
			sUuid = jo.getJSONObject("data").getString("id");
		}catch (Exception ex)	{
			Assert.fail("Cannot get UUID (id) value: " + ex.getMessage() + "\n The json object is: \n" + json);
		}
		return sUuid;
	}
	
	@Test
	public void testRouteGet()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					Map<String, String> mParametersFile = new HashMap<String, String>();
					mParametersFile.put(PARAM_DATA, getPayload("/adminAssetCreateMeta.json").toString());
					//create a file
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setFile("/logo_baasbox_lp.png", "image/png");
					httpRequest(getPostServerRouteAddress(), POST, mParametersFile);
					assertServer(sTestName + " create", Status.CREATED, null, true);
					String uuid1=getUuid();
					
					//get it
					FakeRequest request = new FakeRequest(GET, getStreamFileRouteAddress() + "/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result = routeAndCall(request);
				    Assert.assertEquals(sTestName + " download 1 - status",200,status(result));
					String contentType=play.test.Helpers.header("Content-Type", result);
					Assert.assertEquals(sTestName + " download 1", "image/png", contentType);
					byte[] resultInBytes=JavaResultExtractor.getBody((SimpleResult)result);
					byte[] contentToCheck = getResource("/logo_baasbox_lp.png");
					Assert.assertArrayEquals(sTestName + " check get content",contentToCheck,resultInBytes);
					
					//download it
					request = new FakeRequest(GET, getStreamFileRouteAddress() + "/"+uuid1+"?download=true");
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result = routeAndCall(request);
					String contentDisposition=play.test.Helpers.header("Content-Disposition", result);
					Assert.assertEquals(sTestName + " download 2", "attachment; filename=\"logo_baasbox_lp.png\"", contentDisposition);
					
					//get its attached data
					request = new FakeRequest(GET, getAttachedDataFileRouteAddress() + "/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					Result result1 = routeAndCall(request);
					assertRoute(result1, sTestName + " attachedData", Status.OK, "\"name\":\"Margherita\"", true);
					
					//get its medatata
					String metadataToCheck="\"fileName\":\"logo_baasbox_lp.png\",\"contentType\":\"image/png\",\"contentLength\":70673,\"metadata\":{\"@version\":1,\"Chroma_BlackIsZero\":\"true\",\"Chroma_NumChannels\":\"4\",\"Chroma_ColorSpaceType\":\"RGB\",\"Dimension_ImageOrientation\":\"Normal\",\"tiff_ImageWidth\":\"334\",\"IHDR\":\"width=334, height=112, bitDepth=8, colorType=RGBAlpha, compressionMethod=deflate, filterMethod=adaptive, interlaceMethod=none\",\"Compression_NumProgressiveScans\":\"1\",\"Data_SignificantBitsPerSample\":\"8 8 8 8\",\"tiff_ImageLength\":\"112\",\"tiff_BitsPerSample\":\"8 8 8 8\",\"Compression_CompressionTypeName\":\"deflate\",\"Content-Type\":\"image/png\",\"height\":\"112\",\"Transparency_Alpha\":\"nonpremultipled\",\"X-Parsed-By\":[\"org.apache.tika.parser.DefaultParser\",\"org.apache.tika.parser.image.ImageParser\"],\"pHYs\":\"pixelsPerUnitXAxis=19900, pixelsPerUnitYAxis=19900, unitSpecifier=meter\",\"sBIT_sBIT_RGBAlpha\":\"red=8, green=8, blue=8, alpha=8\",\"UnknownChunks_UnknownChunk\":[\"prVW\",\"mkBF\",\"mkTS\",\"mkBS\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\",\"mkBT\"],\"resourceName\":\"logo_baasbox_lp.png\",\"Compression_Lossless\":\"true\",\"Data_PlanarConfiguration\":\"PixelInterleaved\",\"Dimension_VerticalPixelSize\":\"0.050251257\",\"Dimension_HorizontalPixelSize\":\"0.050251257\",\"width\":\"334\",\"Data_BitsPerSample\":\"8 8 8 8\",\"Text_TextEntry\":\"keyword=Software, value=Macromedia Fireworks 8, encoding=ISO-8859-1, compression=none\",\"Dimension_PixelAspectRatio\":\"1.0\",\"Data_SampleFormat\":\"UnsignedIntegral\",\"tEXt_tEXtEntry\":\"keyword=Software, value=Macromedia Fireworks 8\"}";
					request = new FakeRequest(GET, "/file/details/"+uuid1);
					request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					request = request.withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					result1 = routeAndCall(request);
					assertRoute(result1, sTestName + " MetaData (details)", Status.OK, metadataToCheck, true);				
				}
			}
		);		
	}

	@Test
	public void testServerGet()
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
					httpRequest(getURLAddress(), getMethod());
					assertServer("testServerGet", Status.OK, null, false);
				}
	        }
		);
	}
}
