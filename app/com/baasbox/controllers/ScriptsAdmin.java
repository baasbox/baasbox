/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.controllers;

import java.io.IOException;
import java.util.List;

import play.libs.F;
import play.libs.HttpExecution;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.controllers.actions.filters.CheckAdminRoleFilterAsync;
import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserCredentialWrapFilterAsync;
import com.baasbox.dao.ScriptsDao;
import com.baasbox.dao.exception.ScriptAlreadyExistsException;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptStatus;
import com.baasbox.util.IQueryParametersKeys;
import com.baasbox.util.JSONFormats;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.record.impl.ODocument;



/**
 * Created by Andrea Tortorella on 10/06/14.
 */
@With({UserCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class, CheckAdminRoleFilterAsync.class,ExtractQueryParameters.class})
public class ScriptsAdmin extends Controller{

    private static F.Promise<Result> _activate(String name,boolean activate){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Start Method");
        return F.Promise.promise(()->{
            try {
                DbHelper.openFromContext(ctx());
                return ScriptingService.activate(name,activate);
            }finally {
                DbHelper.close(DbHelper.getConnection());
            }
        }).map( activated -> {
            if (activated==null) {
                return notFound("Script: "+name+" does not exists");
            } else if (activated) {
                return ok("Script: "+name+(activate? " is now active":" is no longer active"));
            } else {
                return  ok("Script already "+name+(activate? " active":" deactivated"));
            }
        }, HttpExecution.defaultContext()).map(ret ->{
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("End Method");
            return ret;
        });

    }
    
    public static F.Promise<Result> activate(String name){
    	return _activate(name,true);
    }
    
    public static F.Promise<Result> deactivate(String name){
    	return _activate(name,false);
    }
    

    @BodyParser.Of(BodyParser.Json.class)
    public static F.Promise<Result> update(String name){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Start Method");
        Http.Request req = request();

        JsonNode body = req.body().asJson();

        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
            Result result;
            try {

                ScriptStatus update = ScriptingService.update(name, body);
                if (update.ok){
                    result = ok(update.message);
                } else {
                    result = badRequest(update.message);
                }
            } catch (ScriptEvalException e) {
            	BaasBoxLogger.error("Evaluation exception: "+e.getMessage(),e);
                result = badRequest(e.getMessage());
            } catch (ScriptException e){
            	BaasBoxLogger.error("Script exception: ",e);
                result = notFound(e.getMessage());
            } catch (Throwable e){
            	BaasBoxLogger.error("Internal Scripts engine error",e);
                result = internalServerError(e.getMessage());
            }
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("End Method");
            return result;
        }));

    }

    @BodyParser.Of(BodyParser.Json.class)
    public static F.Promise<Result> create(){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Start Method");
        Http.Request req = request();
        JsonNode body = req.body().asJson();
        try {
            validateBody(body);
        } catch (ScriptException e) {
            String message = e.getMessage();
            return F.Promise.pure(badRequest(message==null?"Script error: invalid request body":message));
        }
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
            Result result;
            try {

                ScriptStatus res = ScriptingService.create(body);
                if (res.ok){
                    result = created(res.message);
                } else {
                    // todo check return code
                    result = ok(res.message);
                }
            } catch (ScriptAlreadyExistsException e) {
                result = badRequest(e.getMessage());
            }catch (ScriptException e) {
                String message = e.getMessage();

                result = badRequest(message==null?"Script error":message);
            }

            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("End Method");
            return result;
        }));
    }

    private static void validateBody(JsonNode body) throws ScriptException{
        if (body == null){
            throw new ScriptException("missing body");
        }
        JsonNode name = body.get(ScriptsDao.NAME);
        if (name == null || (!name.isTextual())|| name.asText().trim().length()==0) {
            throw new ScriptException("missing required 'name' property");
        }
        JsonNode code = body.get(ScriptsDao.CODE);
        if (code == null||(!code.isTextual())||code.asText().trim().length()==0) {
            throw new ScriptException("missing required 'code' property");
        }
    }

    public static F.Promise<Result> list(){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        QueryParams criteria  = (QueryParams)ctx().args.get(IQueryParametersKeys.QUERY_PARAMETERS);
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
            Result result;
            try {
                List<ODocument> documents = ScriptingService.list(criteria);
                String json = JSONFormats.prepareResponseToJson(documents, JSONFormats.Formats.DOCUMENT);
                result = ok(json);
            } catch (SqlInjectionException e) {
            	BaasBoxLogger.error("Sql injection: ",e);
                result = badRequest(e.getMessage());
            } catch (IOException e) {
            	BaasBoxLogger.error("Error formatting response: ",e);
                result = internalServerError(e.getMessage());
            }
            if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method End");
            return result;
        }));
    }

    public static F.Promise<Result> get(String name){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
       // Result result = null;
        return F.Promise.promise(()->{
            try {
                DbHelper.openFromContext(ctx());
                ODocument script = ScriptingService.get(name);
                return script == null?
                        null:
                        JSONFormats.prepareResponseToJson(script, JSONFormats.Formats.JSON);

            } finally {
                DbHelper.close(DbHelper.getConnection());
            }
        }).map(jdoc -> jdoc == null?notFound("Script: "+name+ " not found")
                                   :ok(jdoc));
    }

    public static F.Promise<Result> drop(String name){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        return F.Promise.promise(()->{
            try {
                DbHelper.openFromContext(ctx());
                return ScriptingService.forceDelete(name);
            }finally {
                DbHelper.close(DbHelper.getConnection());
            }
        }).<Result>map( del -> del?ok():notFound("Script: "+name+ " not found"))
          .<Result>recover( t -> {
              if (t instanceof ScriptException){
            	  BaasBoxLogger.error("Error while deleting script: "+name,t);
                  String message = t.getMessage();
                  return internalServerError(message==null?"Script error":message);
              } else {
                  throw t;
              }
          });
    }

    public static F.Promise<Result> delete(String name){
        if (BaasBoxLogger.isTraceEnabled()) BaasBoxLogger.trace("Method Start");
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
            Result result;
            try {
                boolean deleted =ScriptingService.delete(name);
                if (deleted){
                    result = ok();
                } else {
                    result = notFound("script: "+name+" not found");
                }
            } catch (ScriptException e) {
            	BaasBoxLogger.error("Error while deleting script: "+name,e);
                result = internalServerError(e.getMessage());
            }
            if (BaasBoxLogger.isTraceEnabled())BaasBoxLogger.trace("Method end");
            return result;
        }));


    }
}
