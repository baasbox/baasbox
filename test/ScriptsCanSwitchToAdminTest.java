import com.baasbox.dao.ScriptsDao;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import core.TestConfig;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import play.test.FakeRequest;
import scala.util.parsing.combinator.testing.Str;


import javax.xml.transform.Result;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by eto on 05/10/14.
 */
public class ScriptsCanSwitchToAdminTest   {
    private final static String USER = "user_test_script_sudo-"+ UUID.randomUUID();
    private final static String TEST_SUDO = "test.sudo-"+UUID.randomUUID();

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
                DbHelper.open("1234567890", "admin", "admin");
                createUser();
                ScriptTestHelpers.createScript(TEST_SUDO, "/scripts/test_sudo.js");
            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testCanUserSwitchToAdmin(){
        running(fakeApplication(),()->{
            try {
                String collName = "script-collection-"+UUID.randomUUID();
                ObjectNode node = Json.mapper().createObjectNode();
                node.put("coll",collName);
                FakeRequest req = new FakeRequest(POST, "/script/"+TEST_SUDO);
                req = req.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE);
                req = req.withHeader(TestConfig.KEY_AUTH, TestConfig.encodeAuth(USER, USER));
                req.withJsonBody(node);
                play.mvc.Result result = routeAndCall(req);
                String resultString = contentAsString(result);
                JsonNode resp = Json.mapper().readTree(resultString);
                assertTrue(resp.path("data").path("exists").asBoolean());

            } catch (Exception  e){
                fail(ExceptionUtils.getStackTrace(e));
            }
        });
    }




}
