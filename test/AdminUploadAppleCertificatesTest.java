import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.File;
import java.util.HashMap;

import org.junit.Test;

import play.Play;
import play.libs.F.Callback;
import play.test.TestBrowser;

import com.baasbox.configuration.Push;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.util.ConfigurationFileContainer;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;

import core.AbstractRouteHeaderTest;
import core.TestConfig;


public class AdminUploadAppleCertificatesTest extends AbstractRouteHeaderTest {

	@Override
	public String getRouteAddress() {
		return "/admin/configuration/Push/dummy/profile1.sandbox.ios.certificate/filename.zip";
	}
	
	@Override
	public String getMethod() {
		return "PUT";
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
	@Test
	public void testCorrectUpload() throws Exception{
		
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
					setMultipartFormData();
					setAssetFile("/TestFakeCertificate.p12", "application/octet-stream");
					
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue(status==200);
					File folder =  Play.application().getFile("certificates");
					assertTrue(folder.exists());
					File certificate = Play.application().getFile("certificates/TestFakeCertificate.p12");
					assertTrue(certificate.exists());
					ODatabaseRecordTx db = null;
					try {
						db = DbHelper.open("1234567890", "admin", "admin");
					} catch (InvalidAppCodeException e) {
						fail();
					}
					ODatabaseRecordThreadLocal.INSTANCE.set(db);
					ConfigurationFileContainer cfc = Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValueAsFileContainer();
					assertNotNull(cfc);
					assertNotNull(cfc.getName());
					assertNotNull(cfc.getContent());
					assertEquals("TestFakeCertificate.p12",cfc.getName());
					String content = new String(cfc.getContent());
					assertTrue(content.indexOf("I am a fake certificate")>-1);
					certificate.delete();
					folder.delete();
					db.close();
				}
	        }
		);
	}
	
	

}
