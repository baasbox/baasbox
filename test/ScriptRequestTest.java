import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.TestConfig;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.fest.assertions.Assert;
import org.fest.assertions.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeRequest;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static play.test.Helpers.*;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

/**
 * Created by eto on 1/13/15.
 */
public class ScriptRequestTest {
    private final static String USER = "user_test_script_request-"+ UUID.randomUUID();
    private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();

    private final static String PATH="/test/path";
    private final static String QUERY="?q=2&p=x&p=y";
    private final static String HEADER_KEY="X-Test";
    private final static String HEADER_VALUE="header-val";
    private final static JsonNode QSTRINGJSON = queryStringAsJson();

    private static JsonNode queryStringAsJson(){
        ObjectNode node=Json.mapper().createObjectNode();
        node.put("q",Json.mapper().createArrayNode().add("2"));
        node.put("p",Json.mapper().createArrayNode().add("x").add("y"));
        return node;
    }



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

                ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_request.js");
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
            assertTrue(resp.get("body").isNull());
        });
    }

    private void makeRequest(String method,JsonNode body,Consumer<JsonNode> asserts) {
        running(fakeApplication(),()->{

            FakeRequest req = new FakeRequest(method,"/plugin/"+TEST_CALL+PATH+QUERY);
            req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
            req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(USER,USER));
            req = req.withHeader(HEADER_KEY,HEADER_VALUE);
            if (body != null){
                req=req.withJsonBody(body);
                req = req.withHeader(CONTENT_TYPE,"application/json");
            }
            Result res = route(req);
            String content= contentAsString(res);
            JsonNode node = Json.mapper().readTreeOrMissing(content);
            JsonNode data = node.get("data");
            assertEquals(method,data.get("method").asText());
            assertEquals(PATH.substring(1),data.get("path").asText());
            assertNotNull(data.get("remote"));
            assertEquals(QSTRINGJSON,data.get("queryString"));
            assertEquals(HEADER_VALUE,data.path("headers").path(HEADER_KEY).path(0).asText());
            asserts.accept(data);
        });
    }
}
