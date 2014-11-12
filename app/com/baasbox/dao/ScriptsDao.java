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

package com.baasbox.dao;

import com.baasbox.dao.exception.InvalidScriptException;
import com.baasbox.dao.exception.ScriptAlreadyExistsException;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.orientechnologies.orient.core.command.OCommand;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.OTrackedList;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ODocumentHelper;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.storage.ORecordCallback;
import com.orientechnologies.orient.core.storage.OStorage;
import play.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Andrea Tortorella on 10/06/14.
 */
public class ScriptsDao {
    public static final String MODEL_NAME = "_BB_Script";
    public static final String NAME= "name";
    public static final String CODE= "code";
    public static final String LANG= "lang";
    public static final String LIB= "library";
    public static final String LOCAL_STORAGE= "_storage";
    public static final String INVALID = "_invalid";
    public static final String DATE ="_creation_date";
    public static final String ACTIVE = "active";


    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("([a-zA-Z_][a-zA-Z_0-9]*)(\\.([a-zA-Z][a-zA-Z_0-9]*))+");
    private static final String INDEX =MODEL_NAME+"."+NAME;

    private final ODatabaseRecordTx db;

    protected ScriptsDao(){
        db = DbHelper.getConnection();
    }

    public static ScriptsDao getInstance(){
        return new ScriptsDao();
    }

    public boolean delete(String name){
        ODocument doc = getByName(name);
        if (doc == null) {
            return false;
        } else {
            doc.delete();
            return true;
        }
    }

    //todo update script
    //todo read/update store
    //todo clear log



    public ODocument save(ODocument doc){
        doc.save();
        return doc;
    }


    public ODocument create(String name,String language,String code,boolean isLibrary,boolean active,JsonNode initialStore) throws ScriptException{
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        checkValidName(name);
        if (exists(name)){
            throw new ScriptAlreadyExistsException("Script "+name+" already exists");
        }
        ODocument doc = createPrivileged(name,language,code,isLibrary,active,initialStore);
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return doc;
    }

    private ODocument createPrivileged(String name,String language,String code,boolean isLibrary,boolean active,JsonNode initialStore){
        ODocument doc = makeScript(name, language, code, isLibrary, active, initialStore);
        save(doc);
        return doc;
    }


    public ODocument update(String name, String code) throws ScriptException{
        ODocument doc = getByName(name);
        if (doc == null){
            throw new ScriptException("Script: "+name+" does not exists");
        }
        OTrackedList<String> codeVersions =doc.field(CODE);
        codeVersions.add(0, code);
        save(doc);
        return doc;
    }

    //used by the service
    public void revertToLastVersion(ODocument updated) {
        OTrackedList<String> code = updated.<OTrackedList<String>>field(CODE);
        code.remove(0);
        save(updated);
    }

    private ODocument makeScript(String name, String language, String code, boolean isLibrary, boolean active, JsonNode initialStore) {
        ODocument doc = new ODocument(MODEL_NAME);
        doc.field(NAME,name);
        doc.field(LANG,language);
        List<String> codes = Collections.singletonList(code);
        doc.field(CODE,codes);
        doc.field(DATE,new Date());
        doc.field(LIB,isLibrary);
        doc.field(ACTIVE,active);
        ODocument local = new ODocument();
        if (initialStore!=null){
            local.fromJSON(initialStore.toString());
        }
        doc.field(LOCAL_STORAGE,local);
        doc.field(INVALID,false);
        return doc;
    }

    public List<ODocument> getAll(QueryParams params) throws SqlInjectionException{
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        List<ODocument> docs= null;
        params = params==null?QueryParams.getInstance():params;
        OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME,params.justCountTheRecords(),params);
        docs = DbHelper.commandExecute(command,params.getParams());

        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return docs;
    }

    public List<ODocument> getAll() throws SqlInjectionException {
        return getAll(QueryParams.getInstance());
    }


    public static void checkValidName(String name) throws ScriptException{
        if (name== null||name.trim().length()==0){
            throw new InvalidScriptException("Script must have non empty name");
        }
        if (!VALID_NAME_PATTERN.matcher(name).matches()){
            throw new InvalidScriptException("Script names must be composed of letters numbers and underscores, and cannot start with numbers. Script must have at least one namespace part. Valid example: mynamespace.myscript");
        }
        if (isInternalName(name)){
            throw new InvalidScriptException("User scripts cannot belong to 'baasbox' namespace");
        }
    }

    private static boolean isInternalName(String name){
        return name!=null&&name.startsWith("baasbox");
    }

    public boolean exists(String name){
        if (Logger.isTraceEnabled())Logger.trace("Method Start");
        boolean exists = findByName(name)!= null;
        if (Logger.isDebugEnabled()) Logger.debug("Exists "+exists);
        if (Logger.isTraceEnabled())Logger.trace("Method End");
        return exists;
    }

    public ODocument getByName(String name){
        if (Logger.isTraceEnabled())Logger.trace("Method Start");
        OIdentifiable id = findByName(name);
        ODocument doc  = id == null?null:(ODocument)db.load(id.getIdentity());
        if (Logger.isTraceEnabled())Logger.trace("Method End");
        return doc;
    }

    private OIdentifiable findByName(String name){
        OIndex idx = db.getMetadata().getIndexManager().getIndex(INDEX);
        return (OIdentifiable)idx.get(name);
    }

    public void invalidate(ODocument script) {
        script.field(INVALID,true);
        save(script);
    }

    public ODocument getByNameLocked(String name) {
        OIndex idx = db.getMetadata().getIndexManager().getIndex(INDEX);
        OIdentifiable idf =(OIdentifiable)idx.get(name);
        if (idf == null){
            return null;
        }
        ODocument doc = db.load(idf.getIdentity(), null, false, false, OStorage.LOCKING_STRATEGY.KEEP_EXCLUSIVE_LOCK);

        return doc;
    }

    public boolean activate(ODocument script, boolean activate) {
        boolean current =script.<Boolean>field(ACTIVE);
        script.field(ACTIVE,activate);
        script.save();
        return current!=activate;
    }
}
