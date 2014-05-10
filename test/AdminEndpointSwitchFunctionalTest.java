import com.baasbox.service.permissions.Tags;
import core.AbstractAdminTest;
import core.TestConfig;
import org.apache.http.HttpHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.omg.PortableInterceptor.NON_EXISTENT;
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

    //todo This test is not usable because it skips reading comments from routes.
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
    private static final String INVALID_TAG = "non.existing.tag";

    @Test
    public void testDisableInvalidTag(){
        running(getTestServer(),new Runnable() {
            @Override
            public void run() {
                resetAllHeadersBeforeTests();
                String endpointName = TestConfig.SERVER_URL+"/admin/endpoints/"+INVALID_TAG+"/enabled";
                setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
                setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
                httpRequest(endpointName,PUT);
                assertServer("testServerEnabledEndpoint", Http.Status.NOT_FOUND,null,false);
            }
        });
    }

    @Test
    public void testEnableInvalidTag(){
        running(getTestServer(),new Runnable() {
            @Override
            public void run() {
                resetAllHeadersBeforeTests();
                String endpointName = TestConfig.SERVER_URL+ "/admin/endpoints/"+INVALID_TAG+"/enabled";
                setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
                setHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
                httpRequest(endpointName,PUT);
                assertServer("testServerDisabledInvalidEndpoint", Http.Status.NOT_FOUND,null,false);
            }
        });
    }
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

//    private void routeEnabledSignupShouldPass(){
//        Result res = createUser();
//        assertRoute(res,"routeSignupShouldPass", Http.Status.CREATED,null,false);
//    }

//    private void routeDisabledSignupShouldFail(){
//        Result res = createUser();
//        assertRoute(res,"routeSignupShouldFail", Http.Status.FORBIDDEN,null,false);
//    }

//    private Result createUser(){
//        String endpointName = "/user";
//        String sFakeUser = UserCreateTest.USER_TEST + UUID.randomUUID();
//        // Prepare test user
//        JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
//        FakeRequest request = new FakeRequest(POST,endpointName);
//        request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
//        request = request.withJsonBody(node,POST);
//        return routeAndCall(request);
//    }

//    private void routeEnableUserEndpoint(){
//        String endpointName = Tags.Reserved.ACCOUNT_CREATION.name;
//        FakeRequest request = new FakeRequest(PUT,"/admin/endpoints/"+endpointName+"/enabled");
//        request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
//        request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
//        Result res = routeAndCall(request);
//        assertRoute(res,"testRoute-Enable-endpoint-users", Http.Status.OK,null,false);
//    }

    private void serverDisableUsersEndpoint(){
        resetAllHeadersBeforeTests();
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
        resetAllHeadersBeforeTests();
        String endpointName = TestConfig.SERVER_URL+"/user";
        String sFakeUser = UserCreateTest.USER_TEST + UUID.randomUUID();
        JsonNode node = updatePayloadFieldValue("/adminUserCreatePayload.json", "username", sFakeUser);
        setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        httpRequest(endpointName,POST,node);
    }

    private void serverEnableUsersEndpoint(){
        resetAllHeadersBeforeTests();
        String endpointName = TestConfig.SERVER_URL+"/admin/endpoints/"+Tags.Reserved.ACCOUNT_CREATION.name+"/enabled";
        setHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        setHeader(TestConfig.KEY_AUTH, TestConfig.AUTH_ADMIN_ENC);
        httpRequest(endpointName,PUT);
        assertServer("testServerEnabledEndpoint", Http.Status.OK,null,false);
    }



//    private void routeDisableUsersEndpoint(){
//       String endpointName = Tags.Reserved.ACCOUNT_CREATION.name;
//       FakeRequest request = new FakeRequest(DELETE,"/admin/endpoints/"+endpointName+"/enabled");
//       request = request.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
//       request = request.withHeader(TestConfig.KEY_AUTH,TestConfig.AUTH_ADMIN_ENC);
//       Result res = routeAndCall(request);
//       assertRoute(res,"testRoute-Disable-endpoint-users", Http.Status.OK,null,false);
//   }
}
