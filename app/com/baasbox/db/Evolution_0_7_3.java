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

package com.baasbox.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import play.Logger;

import com.baasbox.configuration.Internal;
import com.baasbox.dao.IndexDao;
import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.RoleService;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.db.ODatabase.ATTRIBUTES;
import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

/**
 * Evolves the DB to the 0.7.3 schema
 * introducing the "File"s
 * 
 * @author Claudio Tesoriero
 *
 */
public class Evolution_0_7_3 implements IEvolution {
	private String version="0.7.3";
	
	public Evolution_0_7_3() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			changeDefaultDateTimeFormat(db);
			fileClassCreation(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	

	private void changeDefaultDateTimeFormat(ODatabaseRecordTx db) {
		Logger.info("..creating _BB_File class..:");
		String[] script=new String[]{
			"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.sssZ;"};
		for (String line:script){
			if (Logger.isDebugEnabled()) Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Logger.info("...done...");
	}

	private void fileClassCreation(ODatabaseRecordTx db) {
		Logger.info("..creating _BB_File class..:");
		String[] script=new String[]{
		"create class _BB_File extends _BB_Node;",
		"create property _BB_File.fileName String;",
		"alter property _BB_File.fileName mandatory=true;",
		"alter property _BB_File.fileName notnull=true;",
		"create property _BB_File.contentType String;",
		"alter property _BB_File.contentType mandatory=true;",
		"alter property _BB_File.contentType notnull=true;",
		"create property _BB_File.contentLength long;",
		"alter property _BB_File.contentLength mandatory=true;",
		"alter property _BB_File.contentLength notnull=true;",
		"create property _BB_File.file link;",
		"alter property _BB_File.file mandatory=true;",
		"alter property _BB_File.file notnull=true;",
		"create class _BB_File_Content;",
		"create property _BB_File_Content.content String;",
		"create index _BB_File_Content.content.key FULLTEXT_HASH_INDEX;"};
		for (String line:script){
			if (Logger.isDebugEnabled()) Logger.debug(line);
			if (!line.startsWith("--") && !line.trim().isEmpty()){ //skip comments
				db.command(new OCommandSQL(line.replace(';', ' '))).execute();
			}
		} 
		Logger.info("...done...");
	}



}
