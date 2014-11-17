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

package com.baasbox.service.scripting;

import com.baasbox.dao.ScriptsDao;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.service.webservices.HttpClientService;
import com.baasbox.service.scripting.base.*;
import com.baasbox.service.scripting.js.Json;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

import play.Logger;
import play.libs.EventSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.exception.ExceptionUtils;
import play.libs.WS;

/**
 * Created by Andrea Tortorella on 10/06/14.
 */
public class ScriptingService {
    private static Map<String,List<EventSource>> connectedLogListeners = new HashMap<String,List<EventSource>>();

    private static final ThreadLocal<String> MAIN = new ThreadLocal<>();

    /*
     * Script lifecycle
     *
     *  |  Install  |
     *       |
     *       v
     *  | Activate |<---.
     *       |          |
     *       v          |
     *    /LIVE/        |
     *       \          |
     *       v          |
     *  | Deactivate |--'
     *       |
     *       v
     *  | Uninstall |
     */

    public static ODocument resetStore(String name,JsonNode data) throws ScriptException {
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = dao.getByName(name);
        if (script == null) throw new ScriptException("Script not found");
        ODocument embedded;
        if (data==null||data.isNull()){
            embedded = null;
            script.removeField(ScriptsDao.LOCAL_STORAGE);
        } else {
            embedded = new ODocument().fromJSON(data.toString());
            script.field(ScriptsDao.LOCAL_STORAGE,embedded);
        }
        dao.save(script);
        return embedded;
    }

    public static ODocument getStore(String name) throws ScriptException {
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = dao.getByName(name);
        if (script == null) throw new ScriptException("Script not found");
        return script.<ODocument>field(ScriptsDao.LOCAL_STORAGE);
    }

    private static ODocument updateStorageLocked(String name,boolean before,JsonCallback updaterFn) throws ScriptException {
        final ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = null;
        try {
            script = dao.getByNameLocked(name);
            if (script == null) throw new ScriptException("Script not found");
            ODocument retScript = before ? script.copy() : script;

            ODocument storage = script.<ODocument>field(ScriptsDao.LOCAL_STORAGE);

            Optional<ODocument> storage1 = Optional.ofNullable(storage);

            JsonNode current = storage1.map(ODocument::toJSON)
                    .map(Json.mapper()::readTreeOrMissing)
                    .orElse(NullNode.getInstance());
            if (current.isMissingNode()) throw new ScriptEvalException("Error reading local storage as json");

            JsonNode updated = updaterFn.call(current);
            ODocument result;
            if (updated ==null||updated.isNull()){
                script.removeField(ScriptsDao.LOCAL_STORAGE);
            } else {
                result = new ODocument().fromJSON(updated.toString());
                script.field(ScriptsDao.LOCAL_STORAGE, result);
            }
            dao.save(script);
            ODocument field = retScript.field(ScriptsDao.LOCAL_STORAGE);
            return field;
        } finally {
            if (script != null) {
                script.unlock();
            }
        }
    }

    public static ODocument swap(String name,JsonCallback callback) throws ScriptException {
        return updateStorageLocked(name,false,callback);
    }

    public static ODocument trade(String name,JsonCallback updater) throws ScriptException {
        return updateStorageLocked(name,true,updater);
    }


    public static List<ODocument> list(QueryParams paramsFromQueryString) throws SqlInjectionException {
        ScriptsDao dao = ScriptsDao.getInstance();
        List<ODocument> scripts = dao.getAll(paramsFromQueryString);
        return scripts;
    }

    public static ScriptStatus update(String name,JsonNode code) throws ScriptException{
        if (code == null) throw new ScriptException("missing code");
        JsonNode codeNode = code.get(ScriptsDao.CODE);
        if (codeNode == null|| !codeNode.isTextual()){
            throw new ScriptException("missing code");
        }
        String source = codeNode.asText();
        return update(name,source);
    }

    public static ScriptStatus update(String name,String code) throws ScriptException {
        ScriptsDao dao = ScriptsDao.getInstance();
        updateCacheVersion();
        ODocument updated = dao.update(name,code);
        compile(updated,false);

        ScriptStatus status;
        ScriptCall install = ScriptCall.install(updated);
        try {
            ScriptResult result = invoke(install);
            status = result.toScriptStatus();
            if (!status.ok){
                updateCacheVersion();
                dao.revertToLastVersion(updated);

            }
        } catch (ScriptEvalException e){
            if (Logger.isDebugEnabled()) Logger.debug("Script installation failed: deleting");
            updateCacheVersion();
            dao.invalidate(updated);
            dao.revertToLastVersion(updated);
            throw e;
        }
        return status;
    }

    /**
     * Creates a new script object
     * @param script
     * @return
     * @throws ScriptException
     */
    public static ScriptStatus create(JsonNode script) throws ScriptException {
        if (Logger.isTraceEnabled()) Logger.trace("Method start");

        if (Logger.isDebugEnabled()) Logger.debug("Creating script");
        ScriptsDao dao = ScriptsDao.getInstance();

        ODocument doc = createScript(dao,script);
        compile(doc,true);
        if (Logger.isDebugEnabled())Logger.debug("Script created");

        if (Logger.isDebugEnabled())Logger.debug("Script installing");
        ScriptStatus status;
        ScriptCall installation = ScriptCall.install(doc);
        try {

            ScriptResult res = invoke(installation);
            status = res.toScriptStatus();
            if (!status.ok){
                if (Logger.isDebugEnabled()) Logger.debug("Script installation aborted by the script");
                doc.delete();
            }
        } catch (ScriptEvalException e){
            if (Logger.isDebugEnabled()) Logger.debug("Script installation failed: deleting - " + ExceptionUtils.getStackTrace(e));
            doc.delete();
            throw new ScriptException(e);
        }
        if (Logger.isTraceEnabled()) Logger.trace("Method end");
        return status;
    }

    /**
     * Returns a script object corresponding to name
     * @param name
     * @return
     */
    public static ODocument get(String name) {
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = dao.getByName(name);
        return script;
    }

    public static ODocument get(String name,boolean onlyvalid,boolean active) throws ScriptException {
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = dao.getByName(name);
        if (script != null){
            if (onlyvalid && script.<Boolean>field(ScriptsDao.INVALID)){
                throw new ScriptException("Script is in invalid state");
            }
            if (active && !(script.<Boolean>field(ScriptsDao.ACTIVE))){
                throw new ScriptEvalException("Script is not active");
            }
        }
        return script;
    }

    /**
     * Deletes a script object with name
     * @param name
     * @return
     */
    public static boolean delete(String name) throws ScriptException{
        updateCacheVersion();
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument script = dao.getByName(name);
        //script not found
        if (script == null){
            return false;
        }

        ScriptCall uninstall = ScriptCall.uninstall(script);
        try {
            invoke(uninstall);
            return dao.delete(name);
        } catch (ScriptException e){
            dao.invalidate(script);
            throw  e;
        }
    }

    public static boolean forceDelete(String name) throws ScriptException {
        updateCacheVersion();
        ScriptsDao dao = ScriptsDao.getInstance();
        return dao.delete(name);
    }


    public static Boolean activate(String name, boolean activate) {
        updateCacheVersion();
        ScriptsDao dao = ScriptsDao.getInstance();
        ODocument doc = dao.getByName(name);
        if (doc == null){
            return null;
        }
        return dao.activate(doc,activate);
    }

    public static ScriptResult invoke(ScriptCall call) throws ScriptEvalException{
        if (Logger.isDebugEnabled()) Logger.debug("Invoking script: " + call.scriptName);
        MAIN.set(call.scriptName);
        BaasboxScriptEngine engine = call.engine();
        try {
            ScriptResult res = engine.eval(call);
            return res;
        } catch (Exception e){
            if (e instanceof ScriptEvalException){
                throw (ScriptEvalException)e;
            } else {
                throw new ScriptEvalException(e.getMessage(),e);
            }
        }finally {
            MAIN.set(null);
        }
    }

//    public static void publishLog(String to,JsonNode message){
//        List<EventSource> listeners = connectedLogListeners.get(to);
//        if (Logger.isTraceEnabled())Logger.trace("Publishing message");
//        if (listeners==null||listeners.isEmpty()) return;
//
//        for (EventSource s:listeners){
//            if (s!=null){
//
//                s.sendData(message.toString());
//            }
//        }
//    }
//
//    public static void disconnectLogListener(String name, EventSource current) {
//        List<EventSource> logs = connectedLogListeners.get(name);
//        if (logs == null){
//            return;
//        } else if (logs.contains(current)){
//            logs.remove(current);
//            if (Logger.isTraceEnabled()) Logger.trace("Disconnected: "+name);
//            connectedLogListeners.put(name,logs);
//        }
//    }

    /**
     * Compiles a script without emitting any event
     * @param doc
     * @throws ScriptEvalException
     */
    private static void compile(ODocument doc,boolean dropOnFailure) throws ScriptEvalException {
        if (Logger.isDebugEnabled()) Logger.debug("Start Compile");
        ScriptCall compile = ScriptCall.compile(doc);
        try {
            invoke(compile);
            if (Logger.isDebugEnabled()) Logger.debug("End Compile");
        } catch (ScriptEvalException e){
            Logger.error("Failed Script compilation");
            if (dropOnFailure){
                doc.delete();
            }else {
                ScriptsDao dao = ScriptsDao.getInstance();

                dao.revertToLastVersion(doc);
            }
            if (Logger.isDebugEnabled()) Logger.debug("Script delete");
            throw e;
        }
    }


    private static ODocument createScript(ScriptsDao dao,JsonNode node) throws ScriptException {
        updateCacheVersion();
        String lang = node.get(ScriptsDao.LANG).asText();
        ScriptLanguage language = ScriptLanguage.forName(lang);
        String name = node.get(ScriptsDao.NAME).asText();
        String code = node.get(ScriptsDao.CODE).asText();
        JsonNode initialStorage = node.get(ScriptsDao.LOCAL_STORAGE);
        JsonNode library = node.get(ScriptsDao.LIB);
        boolean isLibrary =library==null?false:library.asBoolean();
        JsonNode activeNode = node.get(ScriptsDao.ACTIVE);
        boolean active = activeNode == null?false:activeNode.asBoolean();
        ODocument doc = dao.create(name, language.name, code, isLibrary, active, initialStorage);
        return doc;
    }


    public static JsonNode callJsonSync(JsonNode req) throws Exception{
        return callJsonSync(req.get("url").asText(),
                req.get("method").asText(),
                mapJson(req.get("params")),
                mapJson(req.get("headers")),
                req.get("body"));
    }


    private static Map<String,List<String>> mapJson(JsonNode node){
        if (node == null){
            return null;
        }
        if (node.isObject()){
            Map<String,List<String>> ret = new LinkedHashMap<>();
            node.fieldNames().forEachRemaining((field)->{
                JsonNode jsonNode = node.get(field);
                List<String> cur = ret.get(field);
                if (cur == null){
                    cur = new LinkedList<>();
                    ret.put(field, cur);
                }
                append(cur, jsonNode);
            });
            return ret;
        }
        return null;
    }

    private static void append(List<String> list,JsonNode node){
        if (node==null||node.isNull()||node.isMissingNode()||node.isObject()) return;
        if (node.isValueNode()) list.add(node.asText());
        if (node.isArray()){
            node.forEach((n)->{
                if (n!=null && (!n.isNull()) && (!n.isMissingNode()) &&n.isValueNode())list.add(n.toString());
            });
        }
    }

    private static JsonNode callJsonSync(String url,String method,Map<String,List<String>> params,Map<String,List<String>> headers,JsonNode body) throws Exception{
        try {
            ObjectNode node = Json.mapper().createObjectNode();
            WS.Response resp = HttpClientService.callSync(url, method, params, headers, body.isValueNode() ? body.toString() : body);

            int status = resp.getStatus();
            node.put("status",status);

            String header = resp.getHeader("Content-Type");
            if (header==null ||  header.startsWith("text")){
                node.put("body",resp.getBody());
            } else if (header.startsWith("application/json")){
                node.put("body",resp.asJson());
            } else {
                node.put("body",resp.getBody());
            }

            return node;
        } catch (Exception e) {
            Logger.error("failed to connect: "+e.getMessage());
            throw e;
        }

    }


    /// cache management

    private static final AtomicLong SCRIPT_UPDATE_COUNTER = new AtomicLong(Long.MIN_VALUE);

    private static void updateCacheVersion(){
        SCRIPT_UPDATE_COUNTER.incrementAndGet();
    }


    public static long getCacheVersion() {
        return SCRIPT_UPDATE_COUNTER.get();
    }

    public static String main() {
        return MAIN.get();
    }

}
