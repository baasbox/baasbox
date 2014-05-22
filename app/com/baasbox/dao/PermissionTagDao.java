/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

import com.baasbox.controllers.Generic;
import com.baasbox.dao.exception.InvalidPermissionTagException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.dao.exception.PermissionTagAlreadyExistsException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import play.Logger;

import java.util.List;

/**
 *
 * Created by Andrea Tortorella on 08/04/14.
 */
public class PermissionTagDao  {
    private static final String MODEL_NAME ="_BB_Permissions";
    public static final String TAG = "tag";
    public static final String ENABLED = "enabled";
    private static final String INDEX = MODEL_NAME+'.'+TAG;

    private final ODatabaseRecordTx db;
    public static PermissionTagDao getInstance(){
        return new PermissionTagDao();
    }

    protected PermissionTagDao() {
        db = DbHelper.getConnection();
    }


    /**
     * Creates a new tag-permission in the database.
     * The permission starts as enabled.
     * @param name name
     * @return
     * @throws Throwable
     */
    public ODocument create(String name) throws Throwable {
        verifyUnreserved(name);
        return createReserved(name,true);
    }


    /**
     * Creates a new tag-permission in the database
     * @param name name
     * @param enabled start enabled value
     * @return
     * @throws Throwable
     */
    public ODocument create(String name,boolean enabled) throws Throwable {
        verifyUnreserved(name);
        return createReserved(name,enabled);
    }


    /**
     * Creates a new tag-permission in the database, skipping name validation
     * @param name name
     * @param enabled start enabled value
     * @return
     * @throws Throwable
     */
    public ODocument createReserved(String name,boolean enabled) throws Throwable {
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        try {
            if (existsPermissionTag(name)) throw new PermissionTagAlreadyExistsException("name> "+name);
        } catch (SqlInjectionException e){
            throw new InvalidPermissionTagException(e);
        }
        ODocument document = new ODocument(MODEL_NAME);
        document.field(TAG,name);
        document.field(ENABLED,enabled);
        document.save();
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return document;
    }

    /**
     * Creates a new tag-permission in the database, skipping name validation.
     * The permission starts as enabled.
     * @param name name
     * @return
     * @throws Throwable
     */
    public ODocument createReserved(String name) throws Throwable {
        return createReserved(name, true);
    }

    /**
     * Check if the named permission exists
     * @param tagName
     * @return
     * @throws SqlInjectionException
     */
    public boolean existsPermissionTag(String tagName) throws SqlInjectionException{
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        boolean exists = findByName(tagName)!=null;
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return exists;
    }

    /**
     * Gets the specified named permission
     * @param tagName
     * @return
     * @throws SqlInjectionException
     */
    public ODocument getByName(String tagName) throws SqlInjectionException {
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        OIdentifiable record = findByName(tagName);
        ODocument doc = record==null?null:(ODocument)db.load(record.getIdentity());
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return doc;
    }

    /**
     * Enables or disables the named tag-permission
     * @param tagName the name
     * @param enabled the status to set
     * @return true if the status has changed
     * @throws SqlInjectionException
     * @throws com.baasbox.dao.exception.InvalidPermissionTagException
     */
    public boolean setEnabled(String tagName,boolean enabled) throws SqlInjectionException, InvalidPermissionTagException {
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        ODocument doc = getByName(tagName);
        if (doc == null) throw new InvalidPermissionTagException("tag does not exists");
        boolean oldValue=doc.<Boolean>field(ENABLED);
        boolean changed = false;
        if (enabled!=oldValue){
            doc.field(ENABLED,enabled);
            doc.save();
            changed = true;
        }
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return changed;
    }

    /**
     * Gets the enabled status of the permission
     * @param tagName
     * @return
     * @throws SqlInjectionException
     * @throws com.baasbox.dao.exception.InvalidPermissionTagException
     */
    public boolean isEnabled(String tagName) throws SqlInjectionException, InvalidPermissionTagException {
        if (Logger.isTraceEnabled()) Logger.trace("Method Start");
        ODocument doc = getByName(tagName);
        if (doc==null) throw new InvalidPermissionTagException("tag not found");
        boolean enabled = doc.<Boolean>field(ENABLED);
        if (Logger.isTraceEnabled()) Logger.trace("Method End");
        return enabled;
    }

    public List<ODocument> getAll(){
        List<ODocument> docs = null;
        QueryParams criteria = QueryParams.getInstance();
        try {
            OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, false, criteria);
            docs =DbHelper.commandExecute(command,criteria.getParams());
        } catch (SqlInjectionException e) {
            //swallow no injection possible
        }
        return docs;
    }

    //todo implement delete

    private OIdentifiable findByName(String tagName) throws SqlInjectionException{
        OIndex idx = db.getMetadata().getIndexManager().getIndex(INDEX);
        return  (OIdentifiable)idx.get(tagName);
    }


    private void verifyUnreserved(String name) throws InvalidPermissionTagException {
        if (name.startsWith("baasbox.")){
            throw new InvalidPermissionTagException("baasbox.* permissions are reserved");
        }
    }
}
