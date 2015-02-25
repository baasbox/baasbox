package unit;

import static org.junit.Assert.fail;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Test;

import com.baasbox.service.logging.BaasBoxLogger;

import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.util.BBJson;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class TestGrantInTransaction {
    public static final String USER = "admin";
    public static final String COLL = "collssdas"+UUID.randomUUID();


    @Test
    public void testSwitchUser(){
        running(fakeApplication(),
                new Runnable() {
                    @Override
                    public void run() {
                        try {

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
