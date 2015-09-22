package unit;

import static org.junit.Assert.fail;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

import play.mvc.Http;

import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class TestGrantInTransaction {
    public static final String USER = "admin";
    public static final String COLL = "collssdas"+UUID.randomUUID();

    public void setUpContext() throws Exception {
    	Http.Request mockRequest = mock(Http.Request.class);
        when(mockRequest.remoteAddress()).thenReturn("127.0.0.1");
        when(mockRequest.getHeader("User-Agent")).thenReturn("mocked user-agent");
        
        Map<String, String> flashData = Collections.emptyMap();
        Map<String, Object> argData = new HashMap();
        argData.put("appcode", "1234567890");
        Long id = 2L;
        play.api.mvc.RequestHeader header = mock(play.api.mvc.RequestHeader.class);
        Http.Context context = new Http.Context(id, header, mockRequest, flashData, flashData, argData);
        Http.Context.current.set(context);
    }
    
    @Test
    public void testSwitchUser(){
        running(fakeApplication(),
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                        	setUpContext();
                            DbHelper.open("1234567890", USER, USER);
                            ODocument coll = CollectionService.create(COLL);
                            ObjectNode node = BBJson.mapper().createObjectNode();
                            node.put("ciao","ciao");
                            DbHelper.requestTransaction();
                            ODocument doc = DocumentService.create(COLL, node);
                            BaasBoxLogger.debug("**************************************************: "+doc.toJSON("fetchPlan:_links:0"));
                            String rid = DocumentService.getRidByString(doc.field("id"), true);
                            DocumentService.grantPermissionToRole(COLL, rid, Permissions.ALLOW_READ, DefaultRoles.ANONYMOUS_USER.toString());
                            DbHelper.commitTransaction();
                           BaasBoxLogger.debug("**************************************************A:  "+doc.toJSON("fetchPlan:_links:0"));
                        } catch (Throwable e) {
                            BaasBoxLogger.debug(ExceptionUtils.getFullStackTrace(e));
                            fail(ExceptionUtils.getFullStackTrace(e));
                            DbHelper.rollbackTransaction();
                        } finally {
                           

                            DbHelper.close(DbHelper.getConnection());
                        }
                    }
                });

    }
}
