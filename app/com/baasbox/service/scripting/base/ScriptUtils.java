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

import com.baasbox.dao.ScriptsDao;
import com.baasbox.dao.exception.InvalidScriptException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * Created by Andrea Tortorella on 04/07/14.
 */
public final class ScriptUtils {

    private ScriptUtils(){}

    public static void validateScriptParams(JsonNode script) throws InvalidScriptException{
        getAndValidateNameProperty(script);
        getAndValidateScriptLanguage(script);
    }

    public static String getAndvalidateCodeProperty(JsonNode script) throws InvalidScriptException{
        JsonNode code = script.get(ScriptsDao.CODE);
        if (code == null||!code.isTextual()){
            throw new InvalidScriptException("missing code property");
        }
        String c = code.asText();
        if (c==null){
            throw new InvalidScriptException("missing code property");
        }
        return c;
    }

    public static ScriptLanguage getAndValidateScriptLanguage(JsonNode script) throws InvalidScriptException{
        JsonNode langNode = script.get(ScriptsDao.LANG);
        if (langNode==null){
            return ScriptLanguage.JS;
        } else if(!langNode.isTextual()){
            throw new InvalidScriptException("Invalid language: should be a name");
        }else {
            return validateLang(langNode.asText());
        }
    }


    public static String getAndValidateNameProperty(JsonNode script) throws InvalidScriptException{
        JsonNode nameNode = script.get(ScriptsDao.NAME);
        if (nameNode==null||!nameNode.isTextual()){
            throw new InvalidScriptException("Missing name property");
        }
        String name = nameNode.asText();
        validateName(name);
        return name;
    }

    public static ScriptLanguage validateLang(String name) throws InvalidScriptException {
        ScriptLanguage lang = ScriptLanguage.forName(name);
        if(lang==null)
            throw new InvalidScriptException("Invalid language: "+name);
        return lang;
    }

    public static void validateName(String name) throws InvalidScriptException{
        if (name == null||name.trim().length()==0){
            throw new InvalidScriptException("Scripts must have non empty name");
        } else if (name.startsWith("baasbox")){
            throw new InvalidScriptException("Scripts name cannot begin with baasbox");
        } else {
            char c = name.charAt(0);
            if (!(c == '_')||Character.isAlphabetic(c)){
                throw new InvalidScriptException("Script name must begin with '_' or a letter");
            }
        }
    }
}
