/**
 * Created by eto on 25/09/14.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Logger;

import com.baasbox.commands.CommandRegistry;
import com.baasbox.commands.ScriptCommand;
import com.baasbox.commands.ScriptCommands;
import com.baasbox.commands.exceptions.CommandExecutionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.service.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
            }finally{
           	 DbHelper.close(DbHelper.getConnection());
           }
        });
    }

    @Test
    public void testGrantAndRevokeUpdate(){
        running(fakeApplication(),()->{
            try {
            	//initial check. user TEST_ALT_USER cannot update the doc
            	try{
	                DbHelper.open("1234567890",TEST_ALT_USER,TEST_ALT_USER);
		            	ObjectNode paramsUpdate = MAPPER.createObjectNode();
		            	paramsUpdate.put("collection",TEST_COLLECTION);
		            	paramsUpdate.put("id", sGenIds.get(0));
			            paramsUpdate.put("data",MAPPER.readTree("{\"upd\":\"updValue\"}"));
		            	ObjectNode cmdUpdate = ScriptCommands.createCommand("documents", "put",paramsUpdate);
		            	JsonNode nodeUpdate =CommandRegistry.execute(cmdUpdate, null);
	            
	            	DbHelper.close(DbHelper.getConnection());
	            	fail("The user should not update the doc, but it dit it!");
            	}catch (CommandExecutionException e){
            		
            	}catch (Exception e){
            		Logger.debug("OOOPS! something went wrong! ",e);
            		fail(ExceptionUtils.getFullStackTrace(e));
            		throw e;
            	}finally{
            		DbHelper.close(DbHelper.getConnection());
            	}
            	
            	//use TEST_USER grant permission to update the doc to the user TEST_ALT_USER
                DbHelper.open("1234567890",TEST_USER,TEST_USER);
	                ObjectNode params = MAPPER.createObjectNode();
	                ObjectNode users = MAPPER.createObjectNode();
	                ArrayNode update = MAPPER.createArrayNode();
	                update.add(TEST_ALT_USER);
	                users.put("update", update);
	                users.put("read", update);
	                params.put("collection",TEST_COLLECTION);
	                params.put("id", sGenIds.get(0));
	                params.put("users",users);
	                ObjectNode grant = ScriptCommands.createCommand("documents","grant",params);
	                JsonNode node =CommandRegistry.execute(grant, null);
                DbHelper.close(DbHelper.getConnection());

                //now user TEST_ALT_USER can update the doc
                DbHelper.open("1234567890",TEST_ALT_USER,TEST_ALT_USER);
                	ObjectNode paramsUpdate = MAPPER.createObjectNode();
                	paramsUpdate.put("collection",TEST_COLLECTION);
                	paramsUpdate.put("id", sGenIds.get(0));
 	                paramsUpdate.put("data",MAPPER.readTree("{\"generated\":\"generated-123\",\"rand\":123,\"idx\":0,\"upd\":\"updValue\"}"));
                	ObjectNode cmdUpdate = ScriptCommands.createCommand("documents", "put",paramsUpdate);
                	JsonNode nodeUpdate =CommandRegistry.execute(cmdUpdate, null);
                DbHelper.close(DbHelper.getConnection());
                
                //now the grant is revoked
                DbHelper.open("1234567890",TEST_USER,TEST_USER);
	                params = MAPPER.createObjectNode();
	                users = MAPPER.createObjectNode();
	                update = MAPPER.createArrayNode();
	                update.add(TEST_ALT_USER);
	                users.put("update", update);
	                users.put("read", update);
	                params.put("collection",TEST_COLLECTION);
	                params.put("id", sGenIds.get(0));
	                params.put("users",users);
	                grant = ScriptCommands.createCommand("documents","revoke",params);
	                node =CommandRegistry.execute(grant, null);
                DbHelper.close(DbHelper.getConnection());
            }catch (Throwable tr){
            	Logger.debug(ExceptionUtils.getFullStackTrace(tr));
                fail(ExceptionUtils.getFullStackTrace(tr));
            }finally{
            	 DbHelper.close(DbHelper.getConnection());
            }
        });}


    @Test
    public void testGrantAndRevokeRead(){
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
            }finally{
           	 DbHelper.close(DbHelper.getConnection());
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
                ArrayNode params = MAPPER.createArrayNode();
                params.add("5");
                q.put("params",params);
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
