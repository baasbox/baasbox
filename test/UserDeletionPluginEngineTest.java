import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.contentAsString;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractRouteHeaderTest;
import core.TestConfig;

public class UserDeletionPluginEngineTest extends AbstractRouteHeaderTest {

	private final static String TEST_CALL="test.user_drop_"+ScriptTestHelpers.randomScriptName();	

	@Override
	public String getRouteAddress() {
		return "/admin/user";
	}

	@Override
	public String getMethod() {
		return "GET";
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub

	}

	final ObjectMapper _om = new ObjectMapper();

	@BeforeClass
	public static void installPlugin(){
		running(fakeApplication(),()->{
			try {
				DbHelper.open("1234567890", "admin", "admin");
				ScriptTestHelpers.createScript(TEST_CALL, "/scripts/drop_user.js");
			}catch (Throwable e){
				fail(ExceptionUtils.getStackTrace(e));
			} finally {
				DbHelper.close(DbHelper.getConnection());
			}
		});
	}
	
	@Test
	public void dropUserFromPlugin(){
		running(fakeApplication(),()->{
			String toDelete = new AdminCollectionFunctionalTest().createNewUser("toDelete");
			FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL+"?username="+ toDelete);
			req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
			req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
			try {
				req.withJsonBody(BBJson.mapper().readTree("{\"username\":\""+toDelete+"\"}"));
			} catch (Exception e) {
				fail();
			}
			Result res = route(req);
			assertRoute(res,"userDrop.pluginEngine",200,null,false);
			req = new FakeRequest("GET","/admin/user/"+toDelete);
			req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
			req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
			res = route(req);
			assertRoute(res,"userDrop.pluginEngine",404,null,false);
		});
	}

	@Test
	public void dropUserFromPluginWrongCredentials(){
		running(fakeApplication(),()->{
			String toDelete = new AdminCollectionFunctionalTest().createNewUser("toDelete");
			String secondUser = new AdminCollectionFunctionalTest().createNewUser("secondUser");
			FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL+"?username="+ toDelete);
			req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
			req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(secondUser, "passw1"));
			try {
				req.withJsonBody(BBJson.mapper().readTree("{\"username\":\""+toDelete+"\"}"));
			} catch (Exception e) {
				fail();
			}
			Result res = route(req);
			assertRoute(res,"userDrop.pluginEngine",500,null,false);
		});
	}
	
	@Test
	public void dropUserFromPluginUndeletableUser(){
		running(fakeApplication(),()->{
			String secondUser = new AdminCollectionFunctionalTest().createNewUser("secondUser");
			FakeRequest req = new FakeRequest("POST","/plugin/"+TEST_CALL+"?username=admin");
			req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
			req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(secondUser, "passw1"));
			try {
				req.withJsonBody(BBJson.mapper().readTree("{\"username\":\"admin\"}"));
			} catch (Exception e) {
				fail();
			}
			Result res = route(req);
			assertRoute(res,"userDrop.pluginEngine",500,null,false);
		});
	}


}
