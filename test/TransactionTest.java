import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import play.mvc.Http;

import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;


public class TransactionTest {

	private String COLLECTION_NAME="collection_transaction_test_" + UUID.randomUUID();
	
    public void setUpContext() throws Exception {
    	Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.remoteAddress()).thenReturn("127.0.0.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("mocked user-agent");
        
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = new HashMap();
        argData.put("appcode", "1234567890");
        Long id = 2L;
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Context context = new Http.Context(id, header, mockRequest, flashData, flashData, argData);
        Http.Context.current.set(context);
    }
	
	@Before
	public void createTestCollection() throws Exception {
		setUpContext();
		running
		(
			fakeApplication(),
			new Runnable() 
			{
				public void run() 
				{
					try {
						DbHelper.open("1234567890", "admin", "admin");
						CollectionService.create(COLLECTION_NAME);
						DbHelper.close(DbHelper.getConnection());
					} catch (Throwable e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					}
				}
			}
		);
	}
	
	@Test
	public void rollbackTest() {
		running	(
			fakeApplication(),
			new Runnable() 	{
				public void run() 	{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						DbHelper.requestTransaction();
						ObjectNode payload = (ObjectNode) (BBJson.mapper()).readTree("{\"k\":\"v\"}");
						ODocument doc = DocumentService.create(COLLECTION_NAME, payload);
						DbHelper.rollbackTransaction();
						long collCount = DocumentService.getCount(COLLECTION_NAME,QueryParams.getInstance());
						Assert.assertTrue("Collection should be empty. Found " + collCount + " records", collCount==0);
					}catch (Throwable e){
						Assert.fail(ExceptionUtils.getMessage(e));
					}finally{
						DbHelper.close(DbHelper.getConnection());
					}
				}
			});
	}
	
	@Test
	public void commitTest() {
		running	(
			fakeApplication(),
			new Runnable() 	{
				public void run() 	{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						DbHelper.requestTransaction();
						ObjectNode payload = (ObjectNode) (BBJson.mapper()).readTree("{\"k\":\"v\"}");
						ODocument doc = DocumentService.create(COLLECTION_NAME, payload);
						DbHelper.commitTransaction();
						long collCount = DocumentService.getCount(COLLECTION_NAME,QueryParams.getInstance());
						Assert.assertTrue("Collection should contain 1 record. Found " + collCount + " records", collCount==1);
					}catch (Throwable e){
						Assert.fail(ExceptionUtils.getMessage(e));
					}finally{
						DbHelper.close(DbHelper.getConnection());
					}
				}
			});
	}
	
	@Test
	public void  closeWithinTransactionTest() {
		running	(
			fakeApplication(),
			new Runnable() 	{
				public void run() 	{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						DbHelper.requestTransaction();
						ObjectNode payload = (ObjectNode) (BBJson.mapper()).readTree("{\"k\":\"v\"}");
						ODocument doc = DocumentService.create(COLLECTION_NAME, payload);
						DbHelper.close(DbHelper.getConnection());
						Assert.fail("Closing a connection within a transaction should be raise an exception");
					}catch (Throwable e){
						Assert.assertTrue("Closing a connection within an open transaction should be raise a com.baasbox.exception.TransactionIsStillOpenException istead of " + e.getClass().getName() , e.getClass().getName().equals("com.baasbox.exception.TransactionIsStillOpenException"));
					}finally{
						DbHelper.close(DbHelper.getConnection());
					}
				}
			});
	}
	
	@Test
	public void  switchAdminContest() {
		running	(
			fakeApplication(),
			new Runnable() 	{
				public void run() 	{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						DbHelper.requestTransaction();
						DbHelper.reconnectAsAdmin();
						Assert.fail("Switching context within a transaction should be raise an exception");
					}catch (Throwable e){
						Assert.assertTrue("Switching context within an open transaction should be raise a com.baasbox.exception.SwitchUserContextException istead of " + e.getClass().getName() , e.getClass().getName().equals("com.baasbox.exception.SwitchUserContextException"));
					}finally{
						DbHelper.rollbackTransaction();
						DbHelper.close(DbHelper.getConnection());
					}
				}
			});
	}
	
	@Test
	public void  switchuserContest() {
		running	(
			fakeApplication(),
			new Runnable() 	{
				public void run() 	{
					try{
						DbHelper.open("1234567890", "admin", "admin");
						DbHelper.requestTransaction();
						DbHelper.reconnectAsAuthenticatedUser();
						Assert.fail("Switching context within a transaction should be raise an exception");
					}catch (Throwable e){
						Assert.assertTrue("Switching context within an open transaction should be raise a com.baasbox.exception.SwitchUserContextException istead of " + e.getClass().getName() , e.getClass().getName().equals("com.baasbox.exception.SwitchUserContextException"));
					}finally{
						DbHelper.rollbackTransaction();
						DbHelper.close(DbHelper.getConnection());
					}
				}
			});
	}
	
	@After
	public void deleteTestCollection() {
		running
		(
			fakeApplication(),
			new Runnable() 
			{
				public void run() 
				{
					try {
						DbHelper.open("1234567890", "admin", "admin");
						CollectionService.drop(COLLECTION_NAME);
						DbHelper.close(DbHelper.getConnection());
					} catch (Throwable e) {
						Assert.fail(ExceptionUtils.getFullStackTrace(e));
					}
				}
			}
		);
	}
}

