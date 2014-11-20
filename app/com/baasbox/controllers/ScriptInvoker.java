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

import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.ExtractQueryParameters;
import com.baasbox.controllers.actions.filters.UserOrAnonymousCredentialsFilter;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.ScriptCall;
import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptResult;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.lang.exception.ExceptionUtils;
import play.Logger;
import play.libs.EventSource;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import play.data.DynamicForm;
import play.data.Form;

import java.util.Map;

/**
 * Invokes a script through restful api
 *
 * Created by Andrea Tortorella on 10/06/14.
 */
public class ScriptInvoker extends Controller{

    @With({UserOrAnonymousCredentialsFilter.class,
           ConnectToDBFilter.class,
           ExtractQueryParameters.class})
    public static Result invoke(String name,String path){
        ODocument serv = null;
        try {
            serv = ScriptingService.get(name, true,true);
        } catch (ScriptException e) {
           return status(503,"Script is in an invalid state");
        }
        if (serv == null){
            return notFound("Script does not exists");
        }
        JsonNode reqAsJson = serializeRequest(path, request());

        try {
            ScriptResult result =ScriptingService.invoke(ScriptCall.rest(serv, reqAsJson));
            return status(result.status(),result.content());
        } catch (ScriptEvalException e) {
            Logger.error("Error evaluating script",e);
            return internalServerError("script failure "+ ExceptionUtils.getFullStackTrace(e));
        }
//        catch (IllegalStateException e){
//            return internalServerError("script returned invalid json response");
//        }
    }

    public static JsonNode serializeRequest(String path,Http.Request request){

        Http.RequestBody body = request.body();
        String textBody = body==null?null:body.asText();

        DynamicForm requestData = Form.form().bindFromRequest();
        JsonNode jsonBody = Json.mapper().valueToTree(requestData.data());

        Map<String, String[]> headers = request.headers();
        String method = request.method();
        Map<String, String[]> query = request.queryString();
        path=path==null?"/":path;

        ObjectNode reqJson = Json.mapper().createObjectNode();
        reqJson.put("method",method);
        reqJson.put("path",path);

        if(textBody == null)
            reqJson.put("body",jsonBody);
        else
            reqJson.put("body",textBody);

        JsonNode queryJson = Json.mapper().valueToTree(query);
        reqJson.put("queryString",queryJson);
        JsonNode headersJson = Json.mapper().valueToTree(headers);
        reqJson.put("headers",headersJson);
        return reqJson;
    }
}
