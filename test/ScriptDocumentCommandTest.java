/**
 * Created by eto on 25/09/14.
 */

import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.ScriptCommand;
import com.baasbox.commands.ScriptCommands;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.service.user.UserService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.exception.ExceptionUtils;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static play.test.Helpers.*;

public class ScriptDocumentCommandTest {
    private final static String TEST_USER = "script_command_test_user_"+ UUID.randomUUID();
    private final static String TEST_ALT_USER = "script_command_other_"+UUID.randomUUID();

    private final static String TEST_COLLECTION = "script_command_test_coll_"+UUID.randomUUID();
    private static volatile List<String> sGenIds;
    private static final Json.ObjectMapperExt MAPPER = Json.mapper();


    private static List<String> createRandomDocuments(int howMany){
        Random rand = new Random();
        Json.ObjectMapperExt mapper = Json.mapper();
        return IntStream.rangeClosed(0, howMany - 1).mapToObj((x) -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("generated", "generated-" + rand.nextInt());
            node.put("idx", x);
            node.put("rand", rand.nextInt());
            return node;
        }).map((doc) -> {
            try {
                ODocument d = DocumentService.create(TEST_COLLECTION, doc);
                return d.<String>field("id");
            } catch (Throwable throwable) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }


    @BeforeClass
    public static void initTestser(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890","admin","admin");
                ODocument user = UserService.signUp(TEST_USER, TEST_USER, new Date(), null, null, null, null, false);
                assertNotNull(user);
                ODocument alt = UserService.signUp(TEST_ALT_USER,TEST_ALT_USER,new Date(),null,null,null,null,false);
                assertNotNull(alt);

                CollectionService.create(TEST_COLLECTION);
                DbHelper.close(DbHelper.getConnection());
                DbHelper.open("1234567890",TEST_USER,TEST_USER);
                sGenIds = createRandomDocuments(10);
                DbHelper.close(DbHelper.getConnection());
            } catch (Throwable e) {
                fail(ExceptionUtils.getFullStackTrace(e));
            }
        });
    }



    @Test
    public void testGrantAndRevoke(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890",TEST_ALT_USER,TEST_ALT_USER);
                ObjectNode coll = MAPPER.createObjectNode();
                coll.put("collection",TEST_COLLECTION);
                ObjectNode cmd = ScriptCommands.createCommand("documents", "list",coll);
                JsonNode exec = CommandRegistry.execute(cmd, null);
                assertNotNull(exec);
                assertTrue(exec.isArray());
                assertEquals(0,exec.size());
                DbHelper.close(DbHelper.getConnection());

                DbHelper.open("1234567890",TEST_USER,TEST_USER);

                ObjectNode params = MAPPER.createObjectNode();
                ObjectNode users = MAPPER.createObjectNode();
                ArrayNode read = MAPPER.createArrayNode();
                read.add(TEST_ALT_USER);
                users.put("read", read);
                params.put("collection",TEST_COLLECTION);
                params.put("id", sGenIds.get(0));
                params.put("users",users);
                ObjectNode grant = ScriptCommands.createCommand("documents","grant",params);
                JsonNode node =CommandRegistry.execute(grant, null);
                assertNotNull(node);
                assertTrue(node.isBoolean());
                assertTrue(node.asBoolean());

                DbHelper.close(DbHelper.getConnection());

                DbHelper.open("1234567890",TEST_ALT_USER,TEST_ALT_USER);

                JsonNode execWithGrants = CommandRegistry.execute(cmd, null);
                assertNotNull(execWithGrants);
                assertTrue(execWithGrants.isArray());
                assertEquals(1,execWithGrants.size());

                DbHelper.close(DbHelper.getConnection());

                DbHelper.open("1234567890",TEST_USER,TEST_USER);
                ObjectNode revoke = ScriptCommands.createCommand("documents", "revoke", params);
                JsonNode revoked =CommandRegistry.execute(revoke, null);
                assertNotNull(revoked);
                assertTrue(revoked.isBoolean());
                assertTrue(revoked.asBoolean());
                DbHelper.close(DbHelper.getConnection());

                DbHelper.open("1234567890",TEST_ALT_USER,TEST_ALT_USER);

                JsonNode execWithoutGrants = CommandRegistry.execute(cmd, null);
                assertNotNull(execWithoutGrants);
                assertTrue(execWithoutGrants.isArray());
                assertEquals(0,execWithoutGrants.size());

                DbHelper.close(DbHelper.getConnection());

            }catch (Throwable tr){
                fail(ExceptionUtils.getFullStackTrace(tr));
            }
        });
    }

    @Test
    public void testCommandListDocuments(){
        running(fakeApplication(), () -> {
            try {
                DbHelper.open("1234567890", TEST_USER, TEST_USER);
                ObjectNode cmd = MAPPER.createObjectNode();
                ObjectNode p = MAPPER.createObjectNode();
                p.put("collection",TEST_COLLECTION);
                cmd.put(ScriptCommand.RESOURCE,"documents");
                cmd.put(ScriptCommand.NAME,"list");
                cmd.put(ScriptCommand.PARAMS,p);

                JsonNode node =CommandRegistry.execute(cmd, null);

                assertNotNull(node);
                assertTrue(node.isArray());
                assertEquals(10,node.size());
            }catch (Throwable t){
                fail(ExceptionUtils.getFullStackTrace(t));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testCommandGetSingleDocument(){
        running(fakeApplication(), () -> {
            try {
                DbHelper.open("1234567890", TEST_USER, TEST_USER);
                ObjectNode cmd = MAPPER.createObjectNode();
                ObjectNode p = MAPPER.createObjectNode();
                p.put("collection",TEST_COLLECTION);
                p.put("id",sGenIds.get(0));
                cmd.put(ScriptCommand.RESOURCE,"documents");
                cmd.put(ScriptCommand.NAME,"get");
                cmd.put(ScriptCommand.PARAMS,p);

                JsonNode node =CommandRegistry.execute(cmd, null);

                assertNotNull(node);
                assertTrue(node.isObject());
                assertNotNull(node.get("generated"));
                assertNotNull(node.get("id"));
                assertEquals(node.get("id").asText(),sGenIds.get(0));
                assertEquals(node.get("@class").asText(),TEST_COLLECTION);
            }catch (Throwable t){
                fail(ExceptionUtils.getFullStackTrace(t));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testCommandGetFilteredCollection(){
        running(fakeApplication(), () -> {
            try {
                DbHelper.open("1234567890", TEST_USER, TEST_USER);
                ObjectNode cmd = MAPPER.createObjectNode();
                ObjectNode p = MAPPER.createObjectNode();
                ObjectNode q = MAPPER.createObjectNode();
                q.put("where","idx < ?");
                q.put("params",5);
                p.put("collection",TEST_COLLECTION);
                p.put("query",q);

                cmd.put(ScriptCommand.RESOURCE,"documents");
                cmd.put(ScriptCommand.NAME,"list");
                cmd.put(ScriptCommand.PARAMS,p);

                JsonNode node =CommandRegistry.execute(cmd, null);
                assertNotNull(node);
                assertTrue(node.isArray());
                assertEquals(5,node.size());
            }catch (Throwable t){
                fail(ExceptionUtils.getFullStackTrace(t));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testCreateDocument(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890", TEST_USER, TEST_USER);
                ObjectNode params = MAPPER.createObjectNode();
                ObjectNode doc = MAPPER.createObjectNode();
                doc.put("fresh","fresh");
                params.put("collection",TEST_COLLECTION);
                params.put("data",doc);
                ObjectNode cmd = ScriptCommands.createCommand("documents", "post", params);

                JsonNode exec = CommandRegistry.execute(cmd, null);
                assertNotNull(exec);
                assertTrue(exec.isObject());
                assertNotNull(exec.get("id"));
                assertEquals(TEST_COLLECTION,exec.get("@class").asText());


            }catch (Throwable t){
                fail(ExceptionUtils.getFullStackTrace(t));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }
    @Test
    public void testCommandAlterDocument(){
        running(fakeApplication(), () -> {
            try {
                DbHelper.open("1234567890", TEST_USER, TEST_USER);
                ObjectNode cmd = MAPPER.createObjectNode();
                ObjectNode p = MAPPER.createObjectNode();
                p.put("id",sGenIds.get(0));
                p.put("collection",TEST_COLLECTION);

                cmd.put(ScriptCommand.RESOURCE,"documents");
                cmd.put(ScriptCommand.NAME,"get");

                cmd.put(ScriptCommand.PARAMS,p);

                JsonNode node =CommandRegistry.execute(cmd, null);
                assertNotNull(node);
                assertTrue(node.isObject());
                ObjectNode doc = node.deepCopy();
                doc.put("extra","extra");

                ObjectNode upd = MAPPER.createObjectNode();
                upd.put(ScriptCommand.RESOURCE,"documents");
                upd.put(ScriptCommand.NAME,"put");

                ObjectNode params = MAPPER.createObjectNode();
                params.put("collection",TEST_COLLECTION);
                params.put("id",doc.get("id").asText());
                params.put("data",doc);
                upd.put(ScriptCommand.PARAMS,params);
                JsonNode res = CommandRegistry.execute(upd, null);
                assertNotNull(res);
                assertTrue(res.isObject());
                assertNotNull(res.get("extra"));
                assertEquals(res.get("id"),doc.get("id"));
                assertEquals("extra",res.get("extra").asText());
            }catch (Throwable t){
                fail(ExceptionUtils.getFullStackTrace(t));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }


}
