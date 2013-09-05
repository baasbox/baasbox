import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Play;
import play.libs.F.Callback;
import play.test.TestBrowser;

import com.baasbox.BBConfiguration;
import com.baasbox.BBInternalConstants;
import com.baasbox.util.Util;

import core.AbstractRouteHeaderTest;
import core.TestConfig;

public class AdminImportTest extends AbstractRouteHeaderTest {
	
	private static File correctZipFile;
	
	@Override
	public String getRouteAddress() {
		return "/admin/db/import";
	}

	@Override
	public String getMethod() {
		return POST;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
	}
	
	@BeforeClass
	public static void createCorrectFile() throws Exception{
		running(fakeApplication(), new Runnable() {
			
			@Override
			public void run() {
				try{
					String version = BBConfiguration.getApiVersion();
					String fileContent = BBInternalConstants.IMPORT_MANIFEST_VERSION_PREFIX+version;
					String classloaderPath = new File(Play.application().classloader().getResource(".").getFile()).getAbsolutePath();
					File json = Play.application().getFile("test"+File.separator+"resources"+File.separator+"adminImportJson.json");
					File manifest = new File(classloaderPath+File.separator+"target"+File.separator+"manifest.txt");
					FileUtils.writeStringToFile(manifest, fileContent, false);
					
					Util.createZipFile(classloaderPath+File.separator+"adminImportJson.zip",json,manifest);
					//json.delete();
					manifest.delete();
					correctZipFile = new File(Play.application().path().getAbsoluteFile()+File.separator+"target"+File.separator+"adminImportJson.zip");
					
				}catch(Exception e){
					fail();
				}
			}
		});
	}
	
	
	/**
	 * Testing import functionality...while the db is in import mode
	 * the db shouldn't be accessible
	 * @throws Exception
	 */
	@Test
	public void testPostImport() throws Exception
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/adminImportJson.zip", "application/zip");
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue(status==200);
				}
	        }
		);
	}
	
	@Test
	public void testPostFailVersionImport() throws Exception
	{
		running
		(
			testServer(TestConfig.SERVER_PORT), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/adminImportWrongVersionJson.zip", "application/zip");
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue(status!=200);
					
					
				}
	        }
		);
	}
	
	
	@AfterClass
	public static void removeGeneratedFile() throws Exception {
		if(correctZipFile!=null && correctZipFile.exists()){
			correctZipFile.delete();
		}
	}
	
}
