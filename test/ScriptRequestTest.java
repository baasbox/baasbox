import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static play.mvc.Http.HeaderNames.CONTENT_TYPE;
import static play.test.Helpers.GET;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.service.user.UserService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.TestConfig;
import play.mvc.Result;
import play.test.FakeRequest;

/**
 * Created by eto on 1/13/15.
 */
public class ScriptRequestTest {
    private final static String USER = "user_test_script_request-"+ UUID.randomUUID();
    private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();

    private final static String PATH="/test/path/param%2cwith+encoded%20String";
    private final static String QUERY="?q=2&p=x&p=y";
    private final static String HEADER_KEY="X-Test";
    private final static String HEADER_VALUE="header-val";
    private final static JsonNode QSTRINGJSON = queryStringAsJson();

    private static JsonNode queryStringAsJson(){
        ObjectNode node=BBJson.mapper().createObjectNode();
        node.put("q",BBJson.mapper().createArrayNode().add("2"));
        node.put("p",BBJson.mapper().createArrayNode().add("x").add("y"));
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
            //todo disabled null body test
            //assertTrue(resp.get("body").isNull());
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
            JsonNode node = BBJson.mapper().readTreeOrMissing(content);
            JsonNode data = node.get("data");
            assertEquals(method,data.get("method").asText());
            assertEquals("/plugin/" + TEST_CALL + PATH,data.get("path").asText());
            assertEquals(PATH.substring(1),data.get("pathParamsString").asText());
            ArrayNode pathParams = (ArrayNode)data.get("pathParams");
            assertEquals(pathParams.get(2).asText(),"param,with encoded String");
            assertEquals("/plugin/" + TEST_CALL + PATH + QUERY,data.get("uri").asText());
            assertNotNull(data.get("remote"));
            assertNotNull(data.get("remoteAddress"));
            assertNotNull(data.get("pluginName"));
            assertEquals(TEST_CALL,data.get("pluginName").asText());
            assertEquals(QSTRINGJSON,data.get("queryString"));
            assertEquals(HEADER_VALUE,data.path("headers").path(HEADER_KEY).path(0).asText());
            
            asserts.accept(data);
        });
    }
}
