import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.TestConfig;
import play.mvc.Result;
import play.test.FakeRequest;

/**
 * Created by giastfader on 2/16.
 */
public class ScriptFetchPlanTest {
    private final static String TEST_CALL="test.script_fetchplan_"+ScriptTestHelpers.randomScriptName();
   

    @BeforeClass
    public static void initTest(){
        running(fakeApplication(),()->{
            try {
                DbHelper.open("1234567890", "admin", "admin");
                ScriptTestHelpers.createScript(TEST_CALL, "/scripts/test_fetchplan.js");
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
		 	 FakeRequest req = new FakeRequest("GET","/plugin/"+TEST_CALL);
	    	 req = req.withHeader(TestConfig.KEY_APPCODE,TestConfig.VALUE_APPCODE);
	 
			 Result res = route(req);
	         String content= contentAsString(res);
	         try {
				ObjectNode jr = (ObjectNode) BBJson.mapper().readTree(content);
				ObjectNode result1 = (ObjectNode) jr.get("data").get("result1");
				result1.remove("@rid");
				result1.remove("@class");
				result1.remove("id");
				result1.remove("_creation_date");
				
				assertEquals("Error evaluating result1","{\"@version\":1,\"key\":\"value\",\"_author\":\"baasbox\"}",result1.toString());	
				
				//result2: [{"@class":"fetchplan_test","key":"value","id":"030d3043-a7c1-4193-869d-42ecb60efc54","_links":"#11:12","_audit":{"type":"_audit","createdBy":{"@class":"OUser","roles":["#4:4"],"name":"baasbox","status":"ACTIVE"},"createdOn":"2016-02-19T14:49:42.859+0100","modifiedBy":"#5:3","modifiedOn":"2016-02-19T14:49:42.864+0100"},"_allow":["#5:3"],"_allowRead":null,"_allowUpdate":null,"_allowDelete":null,"_creation_date":"2016-02-19T14:49:42.854+0100","_author":"baasbox"}]	
				ArrayNode result2 = (ArrayNode) jr.get("data").get("result2");
				ObjectNode objRes2 = (ObjectNode) result2.get(0);
				assertEquals("Evaluating key: ","value", objRes2.get("key").asText());
				assertEquals("Evaluating 2 _allowRead", true, BBJson.isNull(objRes2.get("_allowRead")));
				assertEquals("Evaluating 2 _allowUpdate", true, BBJson.isNull(objRes2.get("_allowUpdate")));
				assertEquals("Evaluating 2 _allowDelete", true, BBJson.isNull(objRes2.get("_allowDelete")));
				assertEquals("Evaluating 2 _allow", "[\"#5:", objRes2.get("_allow").toString().substring(0, 5));
				
				ObjectNode auditRes2 = (ObjectNode) objRes2.get("_audit");
				assertEquals("_audit 2 type field", "_audit", auditRes2.get("type").asText());
				assertEquals("_audit 2 createdBy field", true, auditRes2.has("createdBy"));
				
				ObjectNode createdByRes2 = (ObjectNode) auditRes2.get("createdBy");
				assertEquals("_audit.createdBy 2 @class field", "OUser", createdByRes2.get("@class").asText());
				
				//result3: [{"@rid":"#24:7","@version":1,"@class":"fetchplan_test","_allow":[{"@rid":"#5:3","@version":20,"@class":"OUser","roles":["#4:4"],"name":"baasbox","status":"ACTIVE"}],"_allowRead":null,"key":"value","id":"a922ff87-44b5-4cde-a4a2-5c5c63434dfc","_allowUpdate":null,"_allowDelete":null,"_author":"baasbox"}]
				ArrayNode result3 = (ArrayNode) jr.get("data").get("result3");
				ObjectNode objRes3 = (ObjectNode) result3.get(0);
				assertEquals("Evaluating 3 key: ","value", objRes3.get("key").asText());
				assertEquals("Evaluating 3 _allowRead", true, BBJson.isNull(objRes3.get("_allowRead")));
				assertEquals("Evaluating 3 _allowUpdate", true, BBJson.isNull(objRes3.get("_allowUpdate")));
				assertEquals("Evaluating 3 _allowDelete", true, BBJson.isNull(objRes3.get("_allowDelete")));
				assertEquals("Evaluating 3 _allow", true, objRes3.get("_allow").isArray());
				assertEquals("Evaluating 3 _allow rid", "\"#5:", objRes3.get("_allow").get(0).get("@rid").toString().substring(0, 4));
				assertEquals("Evaluating 3 _allow class", "\"OUser\"", objRes3.get("_allow").get(0).get("@class").toString());
				assertEquals("Evaluating 3 _allow name", "\"baasbox\"", objRes3.get("_allow").get(0).get("name").toString());
				assertEquals("Evaluating 3 _author", "\"baasbox\"", objRes3.get("_author").toString());
				assertEquals("Evaluating 3 @class", "\"fetchplan_test\"", objRes3.get("@class").toString());
				assertEquals("Evaluating 3 _audit", true, BBJson.isNull(objRes3.get("_audit")));
				
				
				//result4: [{"@class":"fetchplan_test","key":"value","id":"6b7e45e1-4e25-4b6c-aa7e-39c7b23c2e04","_links":"#11:21","_audit":{"type":"_audit","createdBy":{"@class":"OUser","roles":["#4:4"],"name":"baasbox","status":"ACTIVE"},"createdOn":"2016-02-22T16:13:53.735+0100","modifiedBy":"#5:3","modifiedOn":"2016-02-22T16:13:53.743+0100"},"_allow":["#5:3"],"_allowRead":null,"_allowUpdate":null,"_allowDelete":null,"_creation_date":"2016-02-22T16:13:53.731+0100","_author":"baasbox"}]
				ArrayNode result4 = (ArrayNode) jr.get("data").get("result4");
				ObjectNode objRes4 = (ObjectNode) result4.get(0);
				assertEquals("Evaluating 4 key: ","value", objRes4.get("key").asText());
				assertEquals("Evaluating 4 _allowRead", true, BBJson.isNull(objRes4.get("_allowRead")));
				assertEquals("Evaluating 4 _allowUpdate", true, BBJson.isNull(objRes4.get("_allowUpdate")));
				assertEquals("Evaluating 4 _allowDelete", true, BBJson.isNull(objRes4.get("_allowDelete")));
				assertEquals("Evaluating 4 _allow", true, objRes4.get("_allow").isArray());
				assertEquals("Evaluating 4 _allow rid", "[\"#5:", objRes4.get("_allow").toString().substring(0,5));
				assertEquals("Evaluating 4 _author", "\"baasbox\"", objRes4.get("_author").toString());
				assertEquals("Evaluating 4 @class", "\"fetchplan_test\"", objRes4.get("@class").toString());
				assertEquals("Evaluating 4 _audit", false, BBJson.isNull(objRes4.get("_audit")));
				assertEquals("Evaluating 4 _audit object", true, objRes4.get("_audit").isObject());
				assertEquals("Evaluating 4 _audit rid", true, BBJson.isNull(objRes4.get("_audit").get("createdBy").get("@rid")));
				assertEquals("Evaluating 4 _audit class", "\"OUser\"", objRes4.get("_audit").get("createdBy").get("@class").toString());
				assertEquals("Evaluating 4 _audit name", "\"baasbox\"", objRes4.get("_audit").get("createdBy").get("name").toString());
				 
	         } catch (Exception e) {
				fail(ExceptionUtils.getFullStackTrace(e));
		     }
         });
    }

   
}
