import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import play.libs.F.Callback;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;

public class AdminImportTest_issue_428 extends AbstractDocumentTest {
	
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
	public void testPostImportIssue428() throws Exception
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					//load a backup we did using v.0.8.3
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/issue_428_lost_dateformat_import.zip", "application/zip");
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue(status==200);	
					
					//load the document and check the date format
					try {
						serverGetDocument(getURLAddress("test") + "/" + URLEncoder.encode("945edbe6-8f0a-4e0a-9b05-3e93c1038b29", "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						//swallow
						e.printStackTrace();
					}
					assertServer("issue_428 - get document", 200, "", false);
					
					String sCreationDate = getCreationDate(toJSON(getResponse()));
					if (!sCreationDate.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{3}[\\+-]\\d{4}")) {
						 Assert.fail("_creationDate field is in wrong format: " + sCreationDate);
					}
				}
	        }
		);
	}	
}
