import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.user.UserService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


import core.TestConfig;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.fest.assertions.Assert;
import org.fest.assertions.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;


import play.mvc.Result;
import play.test.FakeRequest;


import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;


import static org.junit.Assert.*;
import static play.test.Helpers.*;

/**
 * Created by eto on 1/13/15.
 */
public class ScriptResponseTest {
    private final static String TEST_CALL="test.script_request_"+ScriptTestHelpers.randomScriptName();

    private final static String PATH="/test/path";
    private final static String QUERY="?q=2&p=x&p=y";
    private final static String HEADER_KEY="X-Test";
    private final static String HEADER_VALUE="header-val";
    private final static JsonNode QSTRINGJSON = queryStringAsJson();

    private static JsonNode queryStringAsJson(){
        ObjectNode node=BBJson.mapper().createObjectNode();
        node.put("q",BBJson.mapper().createArrayNode().add("2"));
        node.put("p",BBJson.mapper().createArrayNode().add("x").add("y"));
        return node;
    }



   


    @BeforeClass
    public static void initTest(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890", "admin", "admin");
                ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_response.js");
            }catch (Throwable e){
                fail(ExceptionUtils.getStackTrace(e));
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        });
    }

    @Test
    public void testGet() {
    	 running(fakeApplication(),()->{
    		 String[] testToCall = new String[]{"collection","object","null","nothing","string","empty_string","string_quote","number","decimal","negative","boolean",
    				 //"exp", //java 1.8.0_60 returns a different value. This check is too JVM version dependent
    				 "infinity"};
    		 String[] responseToCheck = new String[]{
    				 "{\"result\":\"ok\",\"data\":[\"hello\",\"world\",42,{\"k\":\"v\",\"o\":3},true],\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":{\"k\":\"v\",\"n\":1},\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":null,\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":\"\",\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":\"Hello World!\",\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":\"\",\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":\"Hello \\\"World!\\\"\",\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":42,\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":45.98,\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":-45.98,\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":false,\"http_code\":200}"
    				 //,"{\"result\":\"ok\",\"data\":2.34E11,\"http_code\":200}"
    				 ,"{\"result\":\"ok\",\"data\":\"Infinity\",\"http_code\":200}"
    				 };
    		 
    		 for (int i=0;i<testToCall.length;i++){
    			 FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL+"?what=" + testToCall[i]);
    			 req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
 
    			 Result res = route(req);
                 String content= contentAsString(res);
                 assertEquals("Error evaluating '" + testToCall[i] + "'. Expected: " + responseToCheck[i] + " got: " + content,
                		 responseToCheck[i],
                		 content);
    		 }
         });
    }

   
}
