import static org.junit.Assert.assertTrue;
import static play.test.Helpers.HTMLUNIT;
import static play.test.Helpers.POST;
import static play.test.Helpers.running;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.sql.OSQLHelper;

import core.AbstractDocumentTest;
import core.TestConfig;
import play.libs.F.Callback;
import play.test.TestBrowser;

public class AdminImportTest_issue_914 extends AbstractDocumentTest {
	
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
	
	
	private String[] imports={"issue_914_evolution_export.zip"};
	
	@Before
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
	
	@Test
	  public void test() throws Exception {
	    running(
	      getFakeApplication(),
	      new Runnable()
	      {
	        public void run()
	        {
	        	try (ODatabaseRecordTx db=DbHelper.open("1234567890", "admin", "admin")){
	        		Object orid=OSQLHelper.parseValue("#3:1", null);
	        		ORecordInternal<?> record = db.load((ORID)orid);
	        		Assert.assertTrue("Record #3:1 has not been deleted by the evolution", record==null);
	        		
	        		orid=OSQLHelper.parseValue("#3:3", null);
	        		record = db.load((ORID)orid);
	        		Assert.assertTrue("Record #3:3 has not been deleted by the evolution", record==null);
	        		
	        		orid=OSQLHelper.parseValue("#3:2", null);
	        		record = db.load((ORID)orid);
	        		Assert.assertTrue("Record #3:3 has been deleted by the evolution", record!=null);
	        		
	        	} catch (Throwable e) {
					Assert.fail(e.getMessage());
				}
	        }
	      }
	      );
	}
}
