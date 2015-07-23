import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.util.HashMap;

import org.junit.Test;

import play.libs.F.Callback;
import play.test.TestBrowser;
import core.AbstractDocumentTest;
import core.TestConfig;

public class AdminImportOldVersionsTest extends AbstractDocumentTest {
	
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
	
	
	private String[] imports={"export-0.9.4.zip","export-1.0.0-M1-from-0.9.4.zip","export-1.0.0-M1.zip","export-1.0.0-M2.zip"};
	
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
					continueOnFail(false);
					for (int i=0;i<imports.length;i++){
						setHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
						setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
						setMultipartFormData();
						setAssetFile("/" + imports[i], "application/zip");
						int status = httpRequest("http://localhost:3333"+getRouteAddress(), getMethod(),new HashMap<String,String>());
						assertTrue(status==200);	
					}
				}
	        }
		);
	}	
}
