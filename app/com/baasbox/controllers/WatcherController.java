package com.baasbox.controllers;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.SessionTokenAccess;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.events.EventSource;
import com.baasbox.service.watchers.WatchKey;
import com.baasbox.service.watchers.WatchService;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.QueryParams;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;

/**
 * Created by eto on 11/14/14.
 */
public class WatcherController extends Controller {

    @With({ExtractQueryParameters.class})
    public static Result registerWatcher(String collection){
        try {
            if (Logger.isTraceEnabled())Logger.trace("Method start");

            SessionTokenAccess session = new SessionTokenAccess();
            boolean credentialsOk = session.setCredential(ctx());
            if (!credentialsOk) {
                return CustomHttpCode.SESSION_TOKEN_EXPIRED.getStatus();
            } else {
                String user = (String)ctx().args.get("username");
                if (user.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername())||
                    user.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())){
                    return forbidden("The user "+user+" cannot access via REST");
                }
            }
            String appcode = (String)ctx().args.get("appcode");
            String user = (String)ctx().args.get("username");
            String passw =(String)ctx().args.get("password");
            try {
                DbHelper.open(appcode, user, passw);
            } catch (InvalidAppCodeException e){
                return badRequest(e.getMessage());
            } finally {
                DbHelper.close(DbHelper.getConnection());
            }

            QueryParams params = (QueryParams)ctx().args.get(IQueryParametersKeys.QUERY_PARAMETERS);
            String throttle = ctx().request().getQueryString("throttle");
            String maxupdate = ctx().request().getQueryString("updates");
            String current=ctx().request().getQueryString("current");

            response().setContentType("text/event-stream");
            return ok(EventSource.source((e)->{
                WatchKey k = WatchService.makeKey(appcode, user, passw, collection, params, throttle, maxupdate, current);
                e.onDisconnected(()-> WatchService.unregisterWatchKey(k));
                WatchService.registerWatchKey(k,e);
            }));

        }finally {
            if (Logger.isTraceEnabled())Logger.trace("Method end");
        }
    }
}
