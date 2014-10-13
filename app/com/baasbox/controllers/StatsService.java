package com.baasbox.controllers;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.SessionTokenAccess;
import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.security.SessionKeys;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import org.apache.commons.lang.StringUtils;
import play.libs.EventSource;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Set;

import static play.mvc.Results.ok;
import static play.mvc.Controller.*;

/**
 * Created by eto on 13/10/14.
 */
public class StatsService {

    public static Result openLogger(){
        SessionTokenAccess sessionTokenAccess = new SessionTokenAccess();
        boolean okCredentials = sessionTokenAccess.setCredential(ctx());
        if (!okCredentials){
            return CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus();
        } else {
            String username =(String) ctx().args.get("username");
            if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername()) ||
                    username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())) {
                return forbidden("The user "+username+" cannot acces via REST");
            }
        }
        String appcode=(String)ctx().args.get("appcode");
        String username=(String)ctx().args.get("username");
        String password=(String)ctx().args.get("password");
        try {
            DbHelper.open(appcode,username,password);
            OUser user = DbHelper.getConnection().getUser();
            Set<ORole> roles = user.getRoles();
            if (!roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()))){
                return forbidden("Logs can only be read by administrators");
            }
        }catch (InvalidAppCodeException e){
            return badRequest(e.getMessage());
        } finally {
            DbHelper.close(DbHelper.getConnection());
        }

        return ok(new EventSource() {
            @Override
            public void onConnected() {
                onDisconnected(() -> {
                    com.baasbox.service.stats.StatsService.removeListener(com.baasbox.service.stats.StatsService.StatType.SCRIPT, this);
                });
                com.baasbox.service.stats.StatsService.addListener(com.baasbox.service.stats.StatsService.StatType.SCRIPT, this);
                sendData("{\"message\": \"start\"}");
            }
        });
    }
}
