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

/***
 * Test for https://github.com/baasbox/baasbox/issues/726
 * Migration problem Evolution to 0.8.4 (from 0.8.2)
 * 
 * Actually there are at least 2 problems: 
 * 1. the one noted into the issue by erayoezmue 
 * 2. a problem with Push settings 
 * 
 * @author geniusatwork
 *
 */
public class AdminImportTest_issue_726 extends AbstractDocumentTest {
	
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
	public void testPostImportIssue726_2() throws Exception
	{
		running
		(
			getTestServer(), 
			HTMLUNIT, 
			new Callback<TestBrowser>() 
	        {
				public void invoke(TestBrowser browser) 
				{
					//load a backup we did using v.0.8.2
					setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
					setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
					setMultipartFormData();
					setAssetFile("/issue_726_import_2_case.zip", "application/zip");
					int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
					assertTrue(status==200);	
					
				}
	        }
		);
	}	
}
