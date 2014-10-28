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

package com.baasbox.service.scripting.js;


import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.ScriptCall;
import com.baasbox.service.scripting.base.ScriptEvalException;
import com.baasbox.service.scripting.base.ScriptResult;
import com.orientechnologies.orient.core.record.impl.ODocument;
import jdk.nashorn.api.scripting.NashornException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang.exception.ExceptionUtils;
import play.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

/**
 * Nashorn engine implementation
 *
 * Created by Andrea Tortorella on 10/06/14.
 */
class Nashorn {
    private static final String MAKE_MODULE_FUNCTION = "makeModule";
    private static final String COMPILE_FUNCTION = "compile";
    private static final String EMIT_FUNCTION = "emit";



    private Map<String,ScriptObjectMirror> cachedModules = new HashMap<String,ScriptObjectMirror>();

    private ScriptEngine mEngine;
    private ScriptObjectMirror mRootAccess;
    private NashornMapper mMapper;
    private long mLocalVersionCount;

    Nashorn(ScriptEngine engine) {
        mEngine = engine;
        mMapper = new NashornMapper();
        mLocalVersionCount = ScriptingService.getCacheVersion();
    }

    /**
     * Initialization logic
     */
    void init() {
        try {
            if(Logger.isDebugEnabled()) Logger.debug("Initializing prelude");
            // get access to prelude and save mirror.
            ScriptObjectMirror mirror = (ScriptObjectMirror)mEngine.eval(ResLoader.jsPrelude());
            mRootAccess = mirror;
            mMapper.setMirror(mRootAccess);
        } catch (ScriptException e){
            //fixme this should never throw
            throw new RuntimeException(e);
        }
    }

    /**
     * Script call evaluation
     * @param call
     * @return
     * @throws ScriptEvalException
     */
    ScriptResult eval(ScriptCall call) throws ScriptEvalException{
        try {
            ScriptObjectMirror moduleRef = getModule(call);

            if (call.event == null) {
                return null;
            }

            Object result = emitEvent(moduleRef, call.event, call.eventData);
            ScriptResult scriptResult = mMapper.convertResult(result);
            call.validate(scriptResult);
            if (Logger.isTraceEnabled())Logger.trace("ScriptResult: %s",scriptResult.toString());
            return scriptResult;
        } catch (Throwable err){
            if (err instanceof NashornException){
                if(Logger.isTraceEnabled())Logger.trace("Error in script");

                Throwable cause = err.getCause();
                NashornException exc =((NashornException) err);
                String scriptStack = NashornException.getScriptStackString(exc);
                scriptStack = ExceptionUtils.getFullStackTrace(exc);
                int columnNumber = exc.getColumnNumber();
                int lineNumber = exc.getLineNumber();
                String fileName = exc.getFileName();
                String message = exc.getMessage();
                String errorMessage = String.format("ScriptError: '%s' at: <%s>%d:%d\n%s",message,fileName,lineNumber,columnNumber,scriptStack);
                throw new ScriptEvalException(errorMessage,err);
            }
            throw new ScriptEvalException(ExceptionUtils.getFullStackTrace(err),err);
        }
    }



    public Object require(String name) {
        if (Logger.isTraceEnabled()) Logger.trace("Required: %s",name);
        syncCache();
        ScriptObjectMirror cached = cachedModules.get(name);
        if (cached == null) {
            try {
                cached = loadModule(name);
            } catch (com.baasbox.dao.exception.ScriptException e) {
                throw new RuntimeException(e);
            }
            if (cached !=null){
                cachedModules.put(name,cached);
            }
        }
        return cached;
    }

    private ScriptObjectMirror loadModule(String name) throws com.baasbox.dao.exception.ScriptException {
        if (name.startsWith("baasbox")){
            return loadIntrinsicModule(name);
        } else {
            return loadUserModule(name);
        }
    }

    private ScriptObjectMirror loadUserModule(String name) throws com.baasbox.dao.exception.ScriptException {
        ODocument doc = ScriptingService.get(name,true,true);
        if (doc == null){
            return null;
        }
        ScriptCall req=ScriptCall.require(doc);
        ScriptObjectMirror mirror = makeModule(req.scriptName, req.source);
        mirror=compileModule(mirror);
        return mirror;
    }

    private ScriptObjectMirror loadIntrinsicModule(String name) {
        String source = ResLoader.loadJsScript(name);
        if (source == null){
            Logger.warn("Module not found");
            return null;
        } else {
            Logger.trace("Module loading");
            ScriptObjectMirror mirror = makeModule(name, source);
            Logger.trace("ModuleReady");
            mirror=compileModule(mirror);
            return mirror;
        }
    }

    private ScriptObjectMirror getModule(ScriptCall call) {
        syncCache();

        ScriptObjectMirror cached = cachedModules.get(call.scriptName);
        if (cached == null){
            if (Logger.isTraceEnabled()) Logger.trace("Loading module: %s",call.scriptName);
            cached = makeModule(call.scriptName,call.source);
            cached = compileModule(cached);
            if(Logger.isTraceEnabled()) Logger.trace("Module compiled: %s",call.scriptName);
            cachedModules.put(call.scriptName,cached);
        }
        return cached;
    }

    private void syncCache() {
        long current = ScriptingService.getCacheVersion();
        if (mLocalVersionCount!=current){
            cachedModules.clear();
            mLocalVersionCount=current;
        }
    }

    private Object emitEvent(ScriptObjectMirror mirror,String eventName,Object eventData) throws ScriptEvalException {
        Object event=mMapper.convertEvent(eventName, eventData);
        Object o = mirror.callMember(EMIT_FUNCTION, event);
        return o;
    }


    private ScriptObjectMirror compileModule(ScriptObjectMirror mirror){
        return (ScriptObjectMirror)mirror.callMember(COMPILE_FUNCTION);
    }

    /**
     * Creates a module from id and source
     * @param id
     * @param sourceCode
     * @return
     */
    private ScriptObjectMirror makeModule(String id,String sourceCode){
        ScriptObjectMirror o =(ScriptObjectMirror) mRootAccess.callMember(MAKE_MODULE_FUNCTION, id, sourceCode);
        return o;
    }

}
