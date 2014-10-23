import com.baasbox.dao.ScriptsDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.AbstractAdminTest;
import core.TestConfig;
import org.apache.http.protocol.HTTP;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;

import javax.ws.rs.core.MediaType;


import java.util.UUID;

import static play.test.Helpers.*;

/**
 *
 * Created by Andrea Tortorella on 18/09/14.
 */
public class AdminCreateScriptsTest extends AbstractAdminTest {
    @Override
    public String getRouteAddress() {
        return "/admin/plugin";
    }

    @Override
    public String getMethod() {
        return POST;
    }

    @Override
    protected void assertContent(String s) {

    }

    @Test
    public void testRouteGetScript() {
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result;
                String e = "test.create"+ScriptTestHelpers.randomScriptName();
                result = routeGetScript(e);
                assertRoute(result, "testRouteGetScript. NoScript", Http.Status.NOT_FOUND, "", true);

                result = routeCreateScript(e,"test.create");
                assertRoute(result,"testRouteGetScript. Create",Http.Status.CREATED,"",true);

                result = routeGetScript(e);
                assertRoute(result,"testRouteGetScript. Get",Http.Status.OK,"on('install',function(e){})",true);

                result = routeDeleteScript(e);
                assertRoute(result,"testRouteGetScript. Delete",Http.Status.OK,"",true);

                result = routeGetScript(e);
                assertRoute(result,"testRouteGetScript. Deleted",Http.Status.NOT_FOUND,"",true);

            }
        });
    }

    @Test //
    public void testUpdateScript(){
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                String ep = "test.update"+ ScriptTestHelpers.randomScriptName();
                Result result = routeCreateScript(ep,"test.update");
                assertRoute(result,"testUpdateScript create",Http.Status.CREATED,null,false);

                Result update = routeUpdateScript(ep,"test.update1");
                assertRoute(update,"testUpdateScript update",Http.Status.OK,"",false);

                result = routeGetScript(ep);
                assertRoute(result,"testUpdateScript get",Http.Status.OK,"function(update1){}",true);
            }
        });
    }


    @Test
    public void testRouteCreateScript(){
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                String s = "test.create"+ScriptTestHelpers.randomScriptName();
                Result create =routeCreateScript(s,"test.create");
                assertRoute(create, "testRouteCreateScript", Http.Status.CREATED, null, false);

                create = routeCreateScript(s,"test.create");
                assertRoute(create,"testRouteCreateScript",Http.Status.BAD_REQUEST,"Script "+s+" already exists",true);

                Result delete = routeDeleteScript(s);
                assertRoute(delete,"testRouteCreateScript. Delete",Http.Status.OK,null,false);

                delete = routeDeleteScript(s);
                assertRoute(delete,"testRouteCreateScript. Delete",Http.Status.NOT_FOUND,null,false);
            }
        });
    }

    @Test
    public void testRouteCreateReservedScript(){
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result;
                result = routeCreateScript("baasbox.forbidden"+UUID.randomUUID(),"baasbox.forbidden");
                assertRoute(result,"testRouteCreateReservedScript",Http.Status.BAD_REQUEST,null,false);
            }
        });
    }


    private Result routeGetScript(String scriptName) {
        FakeRequest request = new FakeRequest(GET,getRouteAddress()+"/"+scriptName);
        request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE)
                .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
        return routeAndCall(request);
    }

    private Result routeDeleteScript(String scriptName) {
        FakeRequest request = new FakeRequest(DELETE,getRouteAddress()+"/"+scriptName);
        request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE)
               .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
        Result result = routeAndCall(request);
        return result;
     }

    private Result routeCreateScript(String name,String script){
        JsonNode payload = getPayload("/scripts/" + script + ".json");
        ObjectNode o = (ObjectNode)payload;
        o.put(ScriptsDao.NAME,name);

        FakeRequest request = new FakeRequest(getMethod(),getRouteAddress());
        request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE)
                         .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC)
                         .withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                         .withJsonBody(o);
        Result result = routeAndCall(request);
        return  result;
    }

    private Result routeUpdateScript(String scriptName, String scriptJson) {
        String addr = getRouteAddress()+"/"+scriptName;
        FakeRequest request = new FakeRequest(PUT,addr);
        request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE)
                .withHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC)
                .withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .withJsonBody(getPayload("/scripts/" + scriptJson + ".json"),PUT);
        Result result = routeAndCall(request);
        return  result;
    }


    @Override
    public void testRouteOK() {
        //super.testRouteOK();
    }

    @Override
    public void testServerOK() {
        //super.testServerOK();
    }
}
