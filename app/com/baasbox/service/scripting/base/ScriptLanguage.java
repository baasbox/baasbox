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

package com.baasbox.service.scripting.base;

import com.baasbox.service.scripting.js.NashornEngine;

/**
 * A scripting language supported by baasbox
 * Created by Andrea Tortorella on 10/06/14.
 */
public enum ScriptLanguage implements ScriptEngineFactory {
    JS("javascript"){
        @Override
        public BaasboxScriptEngine getEngine() {
            return NashornEngine.getNashorn();
        }
    }

    ;

    public final String name;

    ScriptLanguage(String name){
        this.name = name;
    }

    public static ScriptLanguage forName(String lang){
        if (lang == null){
            return JS;
        }
        for (ScriptLanguage l:values()){
            if (l.name.equals(lang)){
                return l;
            }
        }
        return null;
    }
}
