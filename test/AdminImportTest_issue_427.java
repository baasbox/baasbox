import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.io.File;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.Play;
import play.libs.F.Callback;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;

public class AdminImportTest_issue_427 extends AbstractDocumentTest {
	//https://github.com/baasbox/baasbox/issues/427
	//No Push Messages after Migration
	
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
	

	
	
	/**
	 * Testing import functionality...while the db is in import mode
	 * the db shouldn't be accessible
	 * @throws Exception
	 */
	@Test
	public void testPostImportIssue427() throws Exception
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					//deleting old test certificates
					File folder =  Play.application().getFile("certificates");
					if (folder.exists()){
						Logger.info ("Deleting old test certificates...");
						File certificate = Play.application().getFile("certificates/TestFakeCertificateProd.p12");
						if (certificate.exists()) certificate.delete();
						certificate = Play.application().getFile("certificates/TestFakeCertificateSand.p12");
						if (certificate.exists()) certificate.delete();
						Logger.info ("...done");
					}
					
					//load a backup we did using v.0.8.3
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/issue_427_No_Push_Messages_after_Migration.zip", "application/zip");
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue("Import DB Failed! Status: " + status,status==200);	
					
					//check if the certificates have been deployed
					folder =  Play.application().getFile("certificates");
					assertTrue(folder.exists());
					File certificate = Play.application().getFile("certificates/TestFakeCertificateProd.p12");
					assertTrue("TestFakeCertificateProd.p12 not found!",certificate.exists());
					certificate = Play.application().getFile("certificates/TestFakeCertificateSand.p12");
					assertTrue("TestFakeCertificateSand.p12 not found!",certificate.exists());
				}
	        }
		);
	}	
}
