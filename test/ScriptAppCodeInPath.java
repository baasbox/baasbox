import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import play.mvc.Result;
import play.test.FakeRequest;

/**
 * Created by eto on 1/13/15.
 */
public class ScriptAppCodeInPath {
    private final static String USER = "user_test_script_request-"+ UUID.randomUUID();
    private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();

    private final static String PATH="/test/path/";




    @BeforeClass
    public static void initTest(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890", "admin", "admin");

                ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_request.js");
            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testGetWithAppCodeInPath() {
    	makeRequestWithAppCodeInPath(GET,null,(resp)->{});
    }
    
    @Test
    public void testGetWithoutAppCode() {
    	makeRequestWithoutAppCode(GET,null,(resp)->{});
    } 

    private void makeRequestWithoutAppCode(String method, Object body, Consumer<JsonNode> asserts) {
    	running(fakeApplication(),()->{
            FakeRequest req = new FakeRequest(method,"/plugin/"+TEST_CALL+PATH);
            Result res = route(req);
            assertEquals(400,status(res));
    	});
	}

	private void makeRequestWithAppCodeInPath(String method,JsonNode body,Consumer<JsonNode> asserts) {
        running(fakeApplication(),()->{

            FakeRequest req = new FakeRequest(method,"/plugin/"+TEST_CALL+PATH+"/1234567890");
            
            Result res = route(req);
            assertEquals(200,status(res));

        });
    }
}
