import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import core.TestConfig;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeRequest;

import java.util.UUID;

import static play.test.Helpers.*;
import static org.junit.Assert.*;
/**
 * Created by eto on 08/10/14.
 */
public class ScriptStorageTest {
    private static final String SCRIPT = "test.storage-"+ UUID.randomUUID();

    @BeforeClass
    public static void init(){
        running(fakeApplication(),()-> {
            try {
                DbHelper.open("1234567890", "admin", "admin");
                ScriptTestHelpers.createScript(SCRIPT, "scripts/local_storage_test.js");

            } catch (Exception e) {
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testCanStoreValues(){
        running(fakeApplication(),()->{
            try {
                FakeRequest req = new FakeRequest(POST,"/plugin/"+SCRIPT);
                req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE)
                         .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC)
                        .withHeader("Content-Type","application/json")
                        .withJsonBody(Json.mapper().createObjectNode());

                Result res = routeAndCall(req);
                String s = contentAsString(res);
                JsonNode resp =Json.mapper().readTree(s);

                assertEquals(0,resp.path("data").path("val").asInt());

                req = new FakeRequest(POST,"/plugin/"+SCRIPT);
                req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE)
                        .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC)
                        .withHeader("Content-Type","application/json")
                        .withJsonBody(Json.mapper().createObjectNode());

                res = routeAndCall(req);
                s = contentAsString(res);
                resp =Json.mapper().readTree(s);

                assertEquals(1,resp.path("data").path("val").asInt());

                req = new FakeRequest(GET,"/plugin/"+SCRIPT);
                req = req.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE)
                        .withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);



                res = routeAndCall(req);
                s = contentAsString(res);
                resp =Json.mapper().readTree(s);
                assertEquals(1,resp.path("data").path("val").asInt());


            }catch (Exception e){
                fail(ExceptionUtils.getStackTrace(e));
            }
        });
    }

}
