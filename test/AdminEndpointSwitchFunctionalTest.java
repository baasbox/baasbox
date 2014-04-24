import com.baasbox.service.permissions.Tags;
import core.AbstractAdminTest;
import core.TestConfig;
import org.apache.http.HttpHeaders;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.TestBrowser;


import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static play.test.Helpers.*;
/**
 * Created by Andrea Tortorella on 23/04/14.
 */
public class AdminEndpointSwitchFunctionalTest extends AbstractAdminTest {
    @Override
    public String getRouteAddress() {
        return "/admin/endpoints";
    }

    @Override
    public String getMethod() {
        return GET;
    }

    @Override
    protected void assertContent(String s) {

    }

//    @Test
//    public void testRouteOk(){
//        running(getFakeApplication(),new Runnable() {
//            @Override
//            public void run() {
//                routeDisableUsersEndpoint();
//                routeDisabledSignupShouldFail();
//                routeEnableUserEndpoint();
//                routeEnabledSignupShouldPass();
//            }
//        });
//    }

    @Test
    public void testServerOk(){
        running(
                getTestServer(),
                new Runnable() {
                    @Override
                    public void run() {
                        serverDisableUsersEndpoint();
                        serverCreateUserShouldFail();
                        serverEnableUsersEndpoint();
                        serverCreateUserShouldPass();
                    }
                }
        );
    }

    private void routeEnabledSignupShouldPass(){
        Result res = createUser();
        assertRoute(res,"routeSignupShouldPass", Http.Status.CREATED,null,false);
    }

    private void routeDisabledSignupShouldFail(){
        Result res = createUser();
        assertRoute(res,"routeSignupShouldFail", Http.Status.FORBIDDEN,null,false);
    }

    private Result createUser(){
        String endpointName = "/user";
        String sFakeUser = UserCreateTest.USER_TEST + UUID.randomUUID();
        // Prepare test user
        JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
        FakeRequest request = new FakeRequest(POST,endpointName);
        request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        request = request.withJsonBody(node,POST);
        return routeAndCall(request);
    }

    private void routeEnableUserEndpoint(){
        String endpointName = Tags.Reserved.ACCOUNT_CREATION.name;
        FakeRequest request = new FakeRequest(PUT,"/admin/endpoints/"+endpointName+"/enabled");
        request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
        Result res = routeAndCall(request);
        assertRoute(res,"testRoute-Enable-endpoint-users", Http.Status.OK,null,false);
    }

    private void serverDisableUsersEndpoint(){
        String endpointName = TestConfig.SERVER_URL+ "/admin/endpoints/"+Tags.Reserved.ACCOUNT_CREATION.name+"/enabled";
        setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        setHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
        httpRequest(endpointName,DELETE);
        assertServer("testServerDisabledEndpoint", Http.Status.OK,null,false);
    }

    private void serverCreateUserShouldFail(){
        serverCreateUser();
        assertServer("testServerDisabledEndpointFails",Http.Status.FORBIDDEN,null,false);
    }

    private void serverCreateUserShouldPass(){
        serverCreateUser();
        assertServer("testServerDisabledEndpointPass",Http.Status.CREATED,null,false);
    }

    private void serverCreateUser(){
        String endpointName = TestConfig.SERVER_URL+"/user";
        String sFakeUser = UserCreateTest.USER_TEST + UUID.randomUUID();
        JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
        setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        httpRequest(endpointName,POST,node);
    }

    private void serverEnableUsersEndpoint(){
        String endpointName = TestConfig.SERVER_URL+"/admin/endpoints/"+Tags.Reserved.ACCOUNT_CREATION.name+"/enabled";
        setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        setHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
        removeHeader(HttpHeaders.CONTENT_TYPE);
        httpRequest(endpointName,PUT);
        assertServer("testServerEnabledEndpoint", Http.Status.OK,null,false);
    }

    private void routeDisableUsersEndpoint(){
       String endpointName = Tags.Reserved.ACCOUNT_CREATION.name;
       FakeRequest request = new FakeRequest(DELETE,"/admin/endpoints/"+endpointName+"/enabled");
       request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
       request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
       Result res = routeAndCall(request);
       assertRoute(res,"testRoute-Disable-endpoint-users", Http.Status.OK,null,false);
   }
}
