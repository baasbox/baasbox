import static org.junit.Assert.fail;

import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.test.FakeApplication;
import play.test.Helpers;


public class RemoveOrientDBUserTest {

	
	
	@Test
	public void test(){
		FakeApplication fa = Helpers.fakeApplication();
		Helpers.start(fa);
		try (ODatabaseRecordTx db = DbHelper.open("1234567890", "admin", "admin")){
			List<ODocument> result = (List<ODocument>)DbHelper.genericSQLStatementExecute("select from ouser where name in ['reader','writer']", null);
			if (result.size()!=0){
				fail("OrientDB default users are still there! ");
			}
		} catch (InvalidAppCodeException e) {
			fail(ExceptionUtils.getFullStackTrace(e));
		}finally{
			Helpers.stop(fa);
		}
	}
	
}


