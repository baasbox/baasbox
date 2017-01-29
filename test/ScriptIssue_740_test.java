import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static play.test.Helpers.GET;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.db.DbHelper;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;

import core.TestConfig;

/**
 * https://github.com/baasbox/baasbox/issues/740
 * 
 */
public class ScriptIssue_740_test {
    private final static String USER = "user_test_script_request-"+ UUID.randomUUID();
    private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();



    private static void createUser(){
        try {
            UserService.signUp(USER, USER, new Date(), null, null, null, null, false);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }


    @BeforeClass
    public static void initTest(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890", "admin", "admin");
                createUser();

                ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_issue740.js");
            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testGet() {
        makeRequest(GET,null,(resp)->{
            //todo disabled null body test
            //assertTrue(resp.get("body").isNull());
        });
    }

    private void makeRequest(String method,JsonNode body,Consumer<JsonNode> asserts) {
        running(fakeApplication(),()->{

            FakeRequest req = new FakeRequest(method,"/plugin/"+TEST_CALL);
            req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
            req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(USER,USER));
            
            Result res = route(req);
            int rescode=play.test.Helpers.status(res);
            assertEquals("200",Integer.toString(rescode));

        });
    }
}
