import core.AbstractAdminTest;
import core.TestConfig;
import org.apache.http.protocol.HTTP;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;

import javax.ws.rs.core.MediaType;


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
                result = routeGetScript("test.create");
                assertRoute(result, "testRouteGetScript. NoScript", Http.Status.NOT_FOUND, "", true);

                result = routeCreateScript("test.create");
                assertRoute(result,"testRouteGetScript. Create",Http.Status.CREATED,"",true);

                result = routeGetScript("test.create");
                assertRoute(result,"testRouteGetScript. Get",Http.Status.OK,"on('install',function(e){})",true);

                result = routeDeleteScript("test.create");
                assertRoute(result,"testRouteGetScript. Delete",Http.Status.OK,"",true);

                result = routeGetScript("test.create");
                assertRoute(result,"testRouteGetScript. Deleted",Http.Status.NOT_FOUND,"",true);

            }
        });
    }

    @Test //fixme currently failing
    public void testUpdateScript(){
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result result = routeCreateScript("test.update");
                assertRoute(result,"testUpdateScript create",Http.Status.CREATED,null,false);

                Result update = routeUpdateScript("test.update","test.update1");
                assertRoute(update,"testUpdateScript update",Http.Status.OK,"",false);

                result = routeGetScript("test.update");
                assertRoute(result,"testUpdateScript get",Http.Status.OK,"function(update1){}",true);
            }
        });
    }


    @Test
    public void testRouteCreateScript(){
        running(getFakeApplication(), new Runnable() {
            @Override
            public void run() {
                Result create =routeCreateScript("test.create");
                assertRoute(create, "testRouteCreateScript", Http.Status.CREATED, null, false);

                create = routeCreateScript("test.create");
                assertRoute(create,"testRouteCreateScript",Http.Status.BAD_REQUEST,"Script test.create already exists",true);

                Result delete = routeDeleteScript("test.create");
                assertRoute(delete,"testRouteCreateScript. Delete",Http.Status.OK,null,false);

                delete = routeDeleteScript("test.create");
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
                result = routeCreateScript("baasbox.forbidden");
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

    private Result routeCreateScript(String name){
        FakeRequest request = new FakeRequest(getMethod(),getRouteAddress());
        request = request.withHeader(TestConfig.KEY_APPCODE, TestConfig.VALUE_APPCODE)
                         .withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC)
                         .withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                         .withJsonBody(getPayload("/scripts/"+name+".json"));
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
