import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.ScriptCommand;
import com.baasbox.dao.exception.UserAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidJsonException;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static play.test.Helpers.*;
import static org.junit.Assert.*;

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

            try {
                UserService.signUp(user,user,new Date(),visToAnon,visToUser,visToFriends,vistToReg,false);
            } catch (InvalidJsonException|UserAlreadyExistsException e) {
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


//    @Test must be run with http context
    public void testUserCanMakeFriends(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890",sTestUser,sTestUser);
                ObjectNode cmd = mapper.createObjectNode();
                cmd.put(ScriptCommand.RESOURCE,"users");
                cmd.put(ScriptCommand.NAME,"follow");
                ObjectNode params = mapper.createObjectNode();
                params.put("from",sTestUser);
                params.put("to",sRandUsers.first());
                cmd.put(ScriptCommand.PARAMS,params);
                JsonNode exec = CommandRegistry.execute(cmd,null);
                assertNotNull(exec);
                fail(exec.toString());
                assertTrue(exec.isObject());
                assertEquals(sRandUsers.first(),exec.path("user").path("name").asText());
                assertNotNull(exec.get("visibleByFriends"));

            }catch (Throwable e){
                fail(ExceptionUtils.getFullStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }
}
