/**
 * Created by eto on 25/09/14.
 */

import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.ScriptCommand;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.service.user.UserService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicates;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.exception.ExceptionUtils;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static play.test.Helpers.*;

public class ScriptCommandTest  {
    private final static String TEST_USER = "script_command_test_user_"+ UUID.randomUUID();
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


    //@Test uses http context so cannot be run in isolation?
    public void testSwitchUser(){
        running(fakeApplication(),
                new Runnable() {
                    @Override
                    public void run() {
                        try {

                            DbHelper.open("1234567890",TEST_USER,TEST_USER);
                            ObjectNode cmd = MAPPER.createObjectNode();
                            cmd.put(ScriptCommand.RESOURCE,"db");
                            cmd.put(ScriptCommand.MAIN,"gen");
                            cmd.put(ScriptCommand.ID,"gen");
                            cmd.put(ScriptCommand.NAME,"isAdmin");

                            JsonNode exec = CommandRegistry.execute(cmd, null);
                            assertTrue(exec.isBoolean());
                            assertFalse(exec.asBoolean());

                            ObjectNode su = MAPPER.createObjectNode();
                            su.put(ScriptCommand.RESOURCE,"db");
                            su.put(ScriptCommand.ID, "gen");
                            su.put(ScriptCommand.MAIN, "gen");
                            su.put(ScriptCommand.NAME, "switchUser");

                            JsonNode res = CommandRegistry.execute(su, (js) -> {
                                boolean connectedAsAdmin = DbHelper.isConnectedAsAdmin(false);

                                return BooleanNode.valueOf(connectedAsAdmin);
                            });

                            assertNotNull(res);
                            assertTrue(res.isBoolean());
                            assertTrue(res.asBoolean());

                        } catch (Throwable e) {
                            fail(ExceptionUtils.getFullStackTrace(e));
                        } finally {
                            DbHelper.close(DbHelper.getConnection());
                        }
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
                ObjectNode cmd = ScriptCommand.createCommand("documents", "post", params);

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
