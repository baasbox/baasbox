import static play.test.Helpers.running;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

import play.Logger;

import com.baasbox.db.DbHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OResultSet;

import core.AbstractTest;


public class DateFormatTest extends AbstractTest {
	public DateFormatTest() {}

	@Test
	public void testRouteCollectionNotExists()
	{
		running
		(
			getFakeApplication(), 
			new Runnable() 
			{
				public void run() 
				{
					try{
					DbHelper.open("1234567890", "admin", "admin");
					OResultSet result1=(OResultSet)DbHelper.genericSQLStatementExecute("select sysdate()",new String[]{});
					ODocument res1 = (ODocument)result1.get(0);
					String jsonString1= res1.toJSON();
					ObjectMapper om = new ObjectMapper();
					JsonNode json1 = om.readTree(jsonString1);
					String dateString1 = json1.get("sysdate").asText();
					String seconds1=dateString1.substring(17,19);
					String milliseconds1=dateString1.substring(20,23);
					Thread.sleep(1000);
					OResultSet result2=(OResultSet)DbHelper.genericSQLStatementExecute("select sysdate()",new String[]{});
					ODocument res2 = (ODocument)result2.get(0);
					String jsonString2= res2.toJSON();
					JsonNode json2 = om.readTree(jsonString2);
					String dateString2 = json2.get("sysdate").asText();
					String seconds2=dateString2.substring(17,19);
					String milliseconds2=dateString2.substring(20,23);
					
					assertFalse("seconds and milliseconds should be different: " + dateString1 + " " + dateString2, ("0"+seconds1).equals(milliseconds1) && ("0"+seconds2).equals(milliseconds2));
					}catch(Throwable e){
						assertFail(ExceptionUtils.getMessage(e));
						DbHelper.close(DbHelper.getConnection());
					}
				}
			}
		);		
	}

	@Override
	public String getRouteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
}
