
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.POST;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.TestConfig;

/**
 * Created by eto on 05/10/14.
 */
public class ScriptsAdminAndTransactionsTest {
    private final static String USER = "user_test_script_sudo-"+ UUID.randomUUID();
    private final static String TEST_SUDO = "test.sudo."+ScriptTestHelpers.randomScriptName();
    private final static String TEST_TRANSACT= "test.transactions."+ScriptTestHelpers.randomScriptName();
    private static final String TEST_COLLECTION=  "script-transaction-collection-"+UUID.randomUUID();

    private static void createUser(){
        try {
            UserService.signUp(USER,USER,new Date(),null,null,null,null,false);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @BeforeClass
    public static void initTest(){
        running(fakeApplication(),()->{
            try {
            	setUpContext();
                DbHelper.open("1234567890", "admin", "admin");
                createUser();
                CollectionService.create(TEST_COLLECTION);

                ScriptTestHelpers.createScript(TEST_SUDO, "/scripts/test_sudo.js");
                ScriptTestHelpers.createScript(TEST_TRANSACT,"/scripts/run_in_transaction.js");
            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    public static void setUpContext() throws Exception {
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
    public void setContest() throws Exception{
    	setUpContext();
    }
    @Test
    public void canRunCodeTransactionally(){
        running(fakeApplication(),()->{
            try {
                ObjectMapper mapper = BBJson.mapper();
                String operation = "normal";
                ObjectNode node =mapper.createObjectNode();
                node.put("collection",TEST_COLLECTION);
                node.put("op",operation);

                FakeRequest req = new FakeRequest(POST,"/plugin/"+TEST_TRANSACT);

                req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
                req = req.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(USER,USER));
                req =req.withJsonBody(node);
                Result res = routeAndCall(req);
                String content = contentAsString(res);
                JsonNode response = mapper.readTree(content);
                assertEquals(TEST_COLLECTION,response.get("data").get("@class").asText());
                assertNotNull(response.get("data").get("id"));


            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            }
        });
    }




    @Test
    public void testCanUserSwitchToAdmin(){
        running(fakeApplication(),()->{
            try {
                String collName = "script-collection-"+UUID.randomUUID();
                ObjectNode node = BBJson.mapper().createObjectNode();
                node.put("coll",collName);
                FakeRequest req = new FakeRequest(POST, "/plugin/"+TEST_SUDO);
                req = req.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                req = req.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(USER, USER));
                req = req.withJsonBody(node);
                Result result = routeAndCall(req);
                String resultString = contentAsString(result);
                JsonNode resp = BBJson.mapper().readTree(resultString);
                assertTrue(resp.path("data").path("exists").asBoolean());

            } catch (Exception  e){
                fail(ExceptionUtils.getStackTrace(e));
            }
        });
    }




}
