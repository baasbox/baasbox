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

import com.baasbox.service.scripting.base.*;
import play.Logger;

import javax.script.*;

/**
 * Created by Andrea Tortorella on 10/06/14.
 */
public class NashornEngine implements BaasboxScriptEngine {
    private static volatile NashornEngine sEngine;
    private static final Object slock = new Object();


    private static final ThreadLocal<Nashorn> NASHORN = new ThreadLocal<Nashorn>(){
        @Override
        protected Nashorn initialValue() {
            if (Logger.isDebugEnabled())Logger.debug("Creating new nashorn instance");
            ScriptEngine engine = sEngine.newEngine();
            Nashorn nashorn = new Nashorn(engine);
            nashorn.init();
            return nashorn;
        }
    };

    private ScriptEngine newEngine() {
        return nashornFactory.getEngineByName("nashorn");
    }

    private ScriptEngineManager nashornFactory;
    private NashornEngine(){

        nashornFactory = new ScriptEngineManager(null);
    }

    public static NashornEngine getNashorn(){
        if (sEngine == null) {
            synchronized (slock) {
                if (sEngine == null){
                    sEngine = new NashornEngine();
                }
            }
        }
        return sEngine;
    }



    @Override
    public ScriptResult eval(ScriptCall call) throws ScriptEvalException {
        Nashorn evaluator = NASHORN.get();
        try {
            return evaluator.eval(call);
        } catch (ScriptEvalException e){
            Logger.error("Eval failure");
            throw e;
        }
    }


    public Object require(String name) {
        return NASHORN.get().require(name);
    }
}
