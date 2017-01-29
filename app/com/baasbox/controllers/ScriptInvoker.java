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


import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilterAsync;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.ScriptCall;
import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptResult;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;


/**
 * Invokes a script through restful api
 *
 * Created by Andrea Tortorella on 10/06/14.
 */
public class ScriptInvoker extends Controller{


    @With({UserOrAnonymousCredentialsFilterAsync.class,
           ConnectToDBFilterAsync.class,
           ExtractQueryParameters.class})
    public static F.Promise<Result> invoke(String name,String path){
        return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()-> {


            ODocument serv = null;
            if (request().body().asText() != null && request().body().isMaxSizeExceeded())//fixes issue_561
                return badRequest("Too much data! The maximum is " + ObjectUtils.toString(BBConfiguration.configuration.getString("parsers.text.maxLength"), "128KB"));
            try {
                serv = ScriptingService.get(name, true, true);
            } catch (ScriptException e) {
                return status(503, "Script is in an invalid state");
            }
            if (serv == null) {
                return notFound("Script does not exists");
            }
            JsonNode reqAsJson = serializeRequest(path, request());

            try {
                ScriptResult result = ScriptingService.invoke(ScriptCall.rest(serv, reqAsJson));
                return status(result.status(), result.content());
            } catch (ScriptEvalException e) {
            	if (DbHelper.getConnection()!=null && !DbHelper.getConnection().isClosed() && DbHelper.isInTransaction())
            		DbHelper.rollbackTransaction();
            	BaasBoxLogger.error("Error evaluating script", e);
            	return status(CustomHttpCode.PLUGIN_INTERNAL_ERROR.getBbCode(),ExceptionUtils.getFullStackTrace(e));
            }
        }));
    }

    public static JsonNode serializeRequest(String path,Http.Request request){
        Http.RequestBody body = request.body();
       
        Map<String, String[]> headers = request.headers();
        String method = request.method();
        Map<String, String[]> query = request.queryString();
        path=path==null?"/":path;
        ObjectNode reqJson = BBJson.mapper().createObjectNode();
        reqJson.put("method",method);
        reqJson.put("path",path);
        reqJson.put("remote",request.remoteAddress());


        if (!StringUtils.containsIgnoreCase(request.getHeader(CONTENT_TYPE), "application/json")) {
            String textBody = body == null ? null : body.asText();
            if (textBody == null) {
            	//fixes issue 627
               	Map<String, String> params = BodyHelper.requestData(request);
                JsonNode jsonBody = BBJson.mapper().valueToTree(params);
                reqJson.put("body", jsonBody);
            } else {
                reqJson.put("body", textBody);
            }
        } else {
            reqJson.put("body", body.asJson());
        }
        
        JsonNode queryJson = BBJson.mapper().valueToTree(query);
        reqJson.put("queryString",queryJson);
        JsonNode headersJson = BBJson.mapper().valueToTree(headers);
        reqJson.put("headers",headersJson);
        BaasBoxLogger.debug("Serialized request to pass to the script: ");
        BaasBoxLogger.debug(reqJson.toString());
        return reqJson;
    }
}
