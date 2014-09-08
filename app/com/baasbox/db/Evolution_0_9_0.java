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

import play.Logger;

import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.service.user.RoleService;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.security.ORole;

public class Evolution_0_9_0 implements IEvolution {
	private String version="0.9.0";
	
	public Evolution_0_9_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			registeredRoleInheritsFromAnonymousRole(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	//issue #195 Registered users should have access to anonymous resources
	private void registeredRoleInheritsFromAnonymousRole(ODatabaseRecordTx db) {
		Logger.info("...updating registered role");
		ORole regRole = RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString());
		regRole.getDocument().field(RoleDao.FIELD_INHERITED,DefaultRoles.ANONYMOUS_USER.getORole().getDocument().getRecord());
		regRole.save();
		Logger.info("...done");
	}
    
}