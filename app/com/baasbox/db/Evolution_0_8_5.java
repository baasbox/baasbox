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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;

import com.baasbox.configuration.Push;
import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.IndexNotFoundException;
import com.baasbox.util.ConfigurationFileContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.db.record.OTrackedMap;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Evolution_0_8_5 implements IEvolution {
	private String version="0.8.5";
	
	public Evolution_0_8_5() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			createBBNodeIndexes(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}

	private void createBBNodeIndexes(ODatabaseRecordTx db) {
		DbHelper.execMultiLineCommands(db, true, new String[]{
			"create property _BB_Node._author String;",
			"create index _bb_node._author notunique;",
			"create index _bb_node._creation_date notunique;"
		});
	}
	

    
}