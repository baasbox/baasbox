import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import core.TestConfig;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeRequest;

import java.time.Instant;
import java.time.Year;
import java.util.Date;
import java.util.UUID;

import static play.test.Helpers.*;
import static org.junit.Assert.*;
/**
 *
 * Created by eto on 08/10/14.
 */
public class ScriptStorageTest {
    private static final String SCRIPT = "test.storage-"+ UUID.randomUUID();
    private static final String SERIALIZE = "test.serialize-"+UUID.randomUUID();

    @BeforeClass
    public static void init(){
        running(fakeApplication(),()-> {
            try {
                DbHelper.open("1234567890", "admin", "admin");
                ScriptTestHelpers.createScript(SCRIPT, "scripts/local_storage_test.js");
                ScriptTestHelpers.createScript(SERIALIZE,"scripts/serialize_test.js");
            } catch (Exception e) {
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void serializationTest(){
        running(fakeApplication(),()->{
            try {
                FakeRequest req = new FakeRequest(GET,"/plugin/"+ SERIALIZE);
                req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE)
                         .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
                Result res = routeAndCall(req);
                JsonNode result = Json.mapper().readTree(contentAsString(res));
                JsonNode data = result.path("data");
                assertTrue(data.path("integer").isIntegralNumber());
                assertTrue(data.path("double").isDouble());
                assertTrue(data.path("text").isTextual());
                assertTrue(data.path("obj").isObject());
                assertTrue(data.path("ary").isArray());
                assertTrue(data.path("no").isNull());
                assertTrue(data.path("date").asText().startsWith(Year.now().toString()));
                assertEquals("custom",data.path("custom").path("val").asText());
                
            }catch (Exception e){
                fail(ExceptionUtils.getStackTrace(e));
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

                req = new FakeRequest(GET,"/admin/plugin/"+SCRIPT);
                req = req.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE)
                        .withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);

                res = routeAndCall(req);
                s = contentAsString(res);
                resp =Json.mapper().readTree(s);

                assertEquals(1,resp.path("data").path("_store").path("val").asInt());

            }catch (Exception e){
                fail(ExceptionUtils.getStackTrace(e));
            }
        });
    }

}
