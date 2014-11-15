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
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Objects;

/**
 * Created by Andrea Tortorella on 10/06/14.
 */
public class ScriptCall {

    /**
     * Require event
     */
    public static final String REQUIRE = "require";

    /**
     * Install event
     */
    public static final String INSTALL = "install";

    /**
     * Activate event
     */
    public static final String ACTIVATE = "activate";

    /**
     * Deactivate event
     */
    public static final String DEACTIVATE = "deactivate";

    /**
     * Uninstall event
     */
    public static final String UNINSTALL = "uninstall";

    /**
     * Rest service event
     */
    public static final String REQUEST = "request";


    /**
     * Compiles a script
     * @param doc
     * @return
     */
    public static ScriptCall compile(ODocument doc){
        ScriptCall call =new ScriptCall(doc,null,null);
        return call;
    }

    public static ScriptCall require(ODocument doc) {
        ScriptCall call = new ScriptCall(doc,REQUIRE,null);
        return call;
    }

    public static ScriptCall install(ODocument doc) {
        ScriptCall call = new ScriptCall(doc,INSTALL,null);
        return call;
    }

    public static ScriptCall activate(ODocument doc) {
        ScriptCall call = new ScriptCall(doc,ACTIVATE,null);
        return call;
    }

    public static ScriptCall deactivate(ODocument doc) {
        ScriptCall call = new ScriptCall(doc,DEACTIVATE,null);
        return call;

    }

    public static ScriptCall uninstall(ODocument doc){
        ScriptCall call = new ScriptCall(doc,UNINSTALL,null);
        return call;
    }


    public static ScriptCall rest(ODocument serv, JsonNode reqAsJson) {
        ScriptCall call = new ScriptCall(serv,REQUEST,reqAsJson);
        return call;
    }

//    public static ScriptCall require(ScriptLanguage js, String name, String source) {
//        ScriptCall call = new ScriptCall(js,name,source,REQUIRE,null);
//        return call;
//    }

    private final ScriptLanguage language;
    public final String scriptName;
    public final String source;
    public final String event;
    public final Object eventData;

    ScriptCall(ScriptLanguage language,String name,String source,String event,Object eventData){
        this.language=language;
        this.scriptName=name;
        this.source=source;
        this.event=event;
        this.eventData=eventData;
    }

    ScriptCall(ODocument doc,String event,Object eventData){
        this.language = ScriptLanguage.forName(doc.field(ScriptsDao.LANG));
        this.scriptName = doc.field(ScriptsDao.NAME);
        OTrackedList<String> codes = doc.field(ScriptsDao.CODE);
        this.source = codes.get(0);
        this.event=event;
        this.eventData=eventData;
    }

    public BaasboxScriptEngine engine() {
        return language.getEngine();
    }

    public void validate(ScriptResult result) {
       //todo no more validation on responses
    }
}
