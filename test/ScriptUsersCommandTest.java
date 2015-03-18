import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.DELETE;
import static play.test.Helpers.PUT;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.running;

import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.protocol.HTTP;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Logger;
import play.mvc.Result;
import play.test.FakeRequest;

import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.ScriptCommand;
import com.baasbox.commands.exceptions.CommandException;
import com.baasbox.dao.UserDao;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.TestConfig;

/**
 * Created by eto on 29/09/14.
 */
public class ScriptUsersCommandTest {

    private static TreeSet<String>  sRandUsers;
    private static String sTestUser;
    private static final Json.ObjectMapperExt mapper = Json.mapper();
    private static final String USER_PREFIX = "script-users-test-";
    private static String key;

    private static TreeSet<String> createUsers(int howMany){
        Json.ObjectMapperExt mapper = Json.mapper();
        key = UUID.randomUUID().toString();
        return IntStream.range(0, howMany).mapToObj((x)->{
            
            String uuid = UUID.randomUUID().toString();
            String user =USER_PREFIX+ uuid;
            

            ObjectNode visToUser = mapper.createObjectNode();
            visToUser.put("val",x);
            ObjectNode vistToReg = mapper.createObjectNode();
            vistToReg.put("uuid",uuid);
            ObjectNode visToAnon = mapper.createObjectNode();
            visToAnon.put("anon","anon"+x);
            visToAnon.put("key",key);
            ObjectNode visToFriends = mapper.createObjectNode();
            visToFriends.put("friends","friends-"+x);

            ObjectNode param = mapper.createObjectNode();
            param.put("username",user);
            param.put("password", user);
            //test custom ID
            if (x==0)  param.put("id", user + "_0");
            
            param.put(UserDao.ATTRIBUTES_VISIBLE_ONLY_BY_THE_USER,visToUser);
            param.put(UserDao.ATTRIBUTES_VISIBLE_BY_FRIENDS_USER,visToFriends);
            param.put(UserDao.ATTRIBUTES_VISIBLE_BY_REGISTERED_USER,vistToReg);
            param.put(UserDao.ATTRIBUTES_VISIBLE_BY_ANONYMOUS_USER,visToAnon);
            
            ObjectNode cmd = mapper.createObjectNode();
            cmd.put(ScriptCommand.RESOURCE,"users");
            cmd.put(ScriptCommand.NAME,"post");
            cmd.put(ScriptCommand.PARAMS, param);
            
            try {
            	JsonNode exec  = CommandRegistry.execute(cmd,null);
            	Logger.debug(exec.toString());
            	assertTrue(exec.isObject());
            	assertNotNull(exec.get("id"));
            	if (x==0) assertTrue("id is not valid. Expected " + user+"_0" + " received: "+exec.get("id").asText(),exec.get("id").asText().equals(user+"_0"));
                assertNotNull(exec.get("visibleByTheUser"));
                assertNotNull(exec.get("visibleByAnonymousUsers"));
                assertNotNull(exec.get("visibleByRegisteredUsers"));
                assertNotNull(exec.get("visibleByFriends"));
                
                assertNotNull(exec.get("visibleByTheUser").get("val"));
                assertNotNull(exec.get("visibleByAnonymousUsers").get("anon"));
                assertNotNull(exec.get("visibleByRegisteredUsers").get("uuid"));
                assertNotNull(exec.get("visibleByFriends").get("friends"));
                
            	//UserService.signUp(user,user,new Date(),visToAnon,visToUser,visToFriends,vistToReg,false);
            } catch (CommandException e) {
                fail(ExceptionUtils.getFullStackTrace(e));
            }
            return user;

        }).collect(Collectors.toCollection(TreeSet::new));
    }

    @BeforeClass
    public static void initTest(){
        running(fakeApplication(), () -> {
            try {
                DbHelper.open("1234567890", "admin", "admin");
                sRandUsers = createUsers(11);
                sTestUser = sRandUsers.first();
                sRandUsers.remove(sTestUser);
            } catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testUsersFetch(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890",sTestUser,sTestUser);
                ObjectNode cmd = mapper.createObjectNode();
                cmd.put(ScriptCommand.RESOURCE,"users");
                cmd.put(ScriptCommand.NAME,"list");
                ObjectNode param = mapper.createObjectNode();
                param.put("where","visibleByAnonymousUsers.key = ?");
                param.put("params",key);
                cmd.put(ScriptCommand.PARAMS,param);

                JsonNode exec  = CommandRegistry.execute(cmd,null);
                assertTrue(exec.isArray());
                assertEquals(sRandUsers.size() + 1, exec.size());
            }catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testUserCanGetHimself(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890",sTestUser,sTestUser);
                ObjectNode cmd = mapper.createObjectNode();
                cmd.put(ScriptCommand.RESOURCE,"users");
                cmd.put(ScriptCommand.NAME,"get");
                ObjectNode params = mapper.createObjectNode();
                params.put("username",sTestUser);
                cmd.put(ScriptCommand.PARAMS,params);
                JsonNode exec  = CommandRegistry.execute(cmd,null);
                assertTrue(exec.isObject());
                assertEquals(sTestUser, exec.path("user").path("name").asText());
                assertNotNull(exec.get("visibleByTheUser"));
                assertNotNull(exec.get("visibleByAnonymousUsers"));
                assertNotNull(exec.get("visibleByRegisteredUsers"));
                assertNotNull(exec.get("visibleByFriends"));
            }catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            }
        });
    }
    
    @Test
    public void testUserCanUpdateHimself(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890",sTestUser,sTestUser);
                ObjectNode cmd = mapper.createObjectNode();
                cmd.put(ScriptCommand.RESOURCE,"users");
                cmd.put(ScriptCommand.NAME,"put");
                ObjectNode params = mapper.createObjectNode();
                params.put("visibleByTheUser",mapper.createObjectNode().put("private", 1));
                params.put("username",sTestUser);
                cmd.put(ScriptCommand.PARAMS,params);
                JsonNode exec  = CommandRegistry.execute(cmd,null);
                assertTrue(exec.isObject());
                assertEquals(sTestUser, exec.path("user").path("name").asText());
                assertNotNull(exec.get("visibleByTheUser"));
                assertNotNull(exec.get("visibleByAnonymousUsers"));
                assertNotNull(exec.get("visibleByRegisteredUsers"));
                assertNotNull(exec.get("visibleByFriends"));
            }catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            }
        });
    }

    public static Result invokeScript(String scriptName,String user,String pass){
        String endpoint = "/plugin/"+scriptName;
        FakeRequest put = new FakeRequest(PUT,endpoint);
        put.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
        put.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(user,pass));
        return routeAndCall(put);
    }

    @Test
    public void testUserCanMakeFriends(){
        running(fakeApplication(),()->{
            try {
                String scriptName= "makefriends."+ScriptTestHelpers.randomScriptName();
                ScriptTestHelpers.createScript(scriptName, "/scripts/user_make_friends.js");

                ObjectNode user = mapper.createObjectNode();
                user.put("toFollow",sRandUsers.first());

                String endpoint = "/plugin/"+scriptName;
                FakeRequest put = new FakeRequest(PUT,endpoint);
                put.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
                put.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sTestUser,sTestUser));
                put.withHeader(HTTP.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                put.withJsonBody(user,PUT);
                Result invoke = routeAndCall(put);
                String s = contentAsString(invoke);
                JsonNode body = mapper.readTreeOrMissing(s);
                assertEquals("ok",body.get("result").asText());
                assertEquals(sRandUsers.first(),body.path("data").path("user").path("name").asText());
                assertNotNull(body.path("data").path("user").path("visibleByFriends"));

                FakeRequest delete = new FakeRequest(DELETE,endpoint);
                delete.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
                delete.withHeader(TestConfig.KEY_AUTH,TestConfig.encodeAuth(sTestUser,sTestUser));
                delete.withHeader(HTTP.CONTENT_TYPE,MediaType.APPLICATION_JSON);
                delete.withJsonBody(user,DELETE);
                Result dinvoke = routeAndCall(delete);
                String ds = contentAsString(dinvoke);

                JsonNode dbody = mapper.readTreeOrMissing(ds);
                assertTrue(dbody.path("data").asBoolean(false));

            }catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

}
