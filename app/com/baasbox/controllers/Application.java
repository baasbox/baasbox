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

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;


public class Application extends Controller {
  
	  /***
	   * Admin panel web page
	   * @return
	   */
	  public static Result login(){
		  String version = BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION);
		  String edition = BBConfiguration.configuration.getString(IBBConfigurationKeys.EDITION);
		  return ok(views.html.admin.index.render(version,edition));
	  } 
	  
	//renders the spashscreen
  public static Result index() {
	  String version = BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION);
	  String edition = BBConfiguration.configuration.getString(IBBConfigurationKeys.EDITION);
	  return ok(views.html.index.render(version,edition));
  }


  public static Result apiVersion() {
	  ObjectNode result = Json.newObject();
	  result.put("api_version", BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION));
	  result.put("edition", BBConfiguration.configuration.getString(IBBConfigurationKeys.API_VERSION));
	  return ok(result);
  }
  

}