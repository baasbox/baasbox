/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.controllers;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.baasbox.db.hook.HooksManager;
import com.orientechnologies.orient.core.db.graph.OGraphDatabase;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.security.ODatabaseSecurityResources;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;


public class Application extends Controller {
  
  public static Result index() {
	  String version = BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION);
	  return ok(views.html.index.render(version));
  }
  
  public static Result apiVersion() {
	  response().setContentType("application/json");
	    return ok("{\"api_version\":"+BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION)+"}");
	  }
  
  /**
   * controller function used to test and to make some experiment
   * @return
   */
  public static Result test() {
		//create the new role for the friends
	  Logger.trace("Method Start");
	  	OGraphDatabase db=new OGraphDatabase("local:" + BBConfiguration.getDBDir()).open(IBBConfigurationKeys.ADMIN_USERNAME, IBBConfigurationKeys.ADMIN_PASSWORD);
		HooksManager.registerAll(db);
		
		db.begin(TXTYPE.OPTIMISTIC);
		final ORole role =  db.getMetadata().getSecurity().createRole("test", ORole.ALLOW_MODES.DENY_ALL_BUT);
		role.addRule(ODatabaseSecurityResources.DATABASE, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.SCHEMA, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.CLUSTER + "." + OMetadata.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.CLUSTER + ".orole", ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.CLUSTER + ".ouser", ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.ALL_CLASSES, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.ALL_CLUSTERS, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.COMMAND, ORole.PERMISSION_READ);
		role.addRule(ODatabaseSecurityResources.RECORD_HOOK, ORole.PERMISSION_READ);
	      
	    role.save();
		db.commit();
		db.close();
		Logger.trace("Method Start");
		return ok();
  }
  
  /***
   * Admin panel web page
   * @return
   */
  public static Result login(){
	  String version = BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION);
	  return ok(views.html.admin.index.render(version));
  } 

}