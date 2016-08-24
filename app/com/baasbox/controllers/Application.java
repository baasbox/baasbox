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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.baasbox.BBConfiguration;
import com.baasbox.IBBConfigurationKeys;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.api.templates.Html;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;


public class Application extends Controller {
  
	  /***
	   * Admin panel web page
	   * @return
	   */
	  public static Result login(){
		  String version = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.API_VERSION);
		  String edition = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.EDITION);
		  return ok(views.html.admin.index.render(version,edition));
	  } 

	  private static Html getDefaultIndexFile(){
		  String version = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.API_VERSION);
		  String edition = BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.EDITION);
		  return views.html.index.render(version,edition);
	  }
	  
	/***
	 * renders the splashscreen or the default index file into the www folder
	 * @return
	 */
	  public static Result index() {
		  if (BBConfiguration.getInstance().isWebEnabled()) {
			  Path wwwDir = Paths.get(BBConfiguration.getInstance().getWebPath());
			  Optional<String> index = BBConfiguration.getInstance().getWebIndexFiles().stream().filter(indexFileName ->{
				  Path fileToReturn = wwwDir.resolve(indexFileName);
				  return Files.exists(fileToReturn);
			  }).findFirst();
			  if (index.isPresent()){
				  response().setHeader("Content-Disposition","inline"); 
				  return ok(wwwDir.resolve(index.get()).toFile());
			  } else {
				  return ok(getDefaultIndexFile());
			  }
		  }
		  return ok(getDefaultIndexFile());
	  } //index()

	  public static Result apiVersion() {
		  ObjectNode result = Json.newObject();
		  result.put("api_version", BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.API_VERSION));
		  result.put("edition", BBConfiguration.getInstance().configuration.getString(IBBConfigurationKeys.EDITION));
		  return ok(result);
	  }
  

}