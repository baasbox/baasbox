package com.baasbox.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.baasbox.BBConfiguration;
import com.baasbox.service.logging.BaasBoxLogger;

import play.mvc.Controller;
import play.mvc.Result;

public class Web extends Controller {
	
	/***
	 * Returns the content of the specified file or redirects the client to load the file of the embedded console
	 * The index.html file (or the one specified into the application.conf file is processed by the Application.java controller
	 * @param uri
	 * @return
	 */
	 public static Result getFile(String uri) {
		  if (BBConfiguration.getInstance().isWWWEnabled()){
			  Path wwwDir = Paths.get(BBConfiguration.getInstance().getWWWPath());
			  Path fileToReturn = wwwDir.resolve(uri);
			  boolean fileExists = Files.exists(fileToReturn);
			  if (fileExists){ //the requested file has been found into the web folder
				  File file = fileToReturn.toFile();
				  if (!file.isDirectory()) { 
					  BaasBoxLogger.debug("WWW file found: " + uri);
					  response().setHeader("Content-Disposition","inline"); 
					  return ok(fileToReturn.toFile()); //returns the file
				  } else { 
					  //if the URI is a folder/directory, then search for the index.html file
					  BaasBoxLogger.debug("The uri is a folder: " + uri);
					  Optional<String> index = BBConfiguration.getInstance().getIndexFiles().stream().filter(indexFileName ->{
						  Path xPath = wwwDir.resolve(indexFileName);
						  return Files.exists(xPath);
					  }).findFirst();
					  if (index.isPresent()){
						  response().setHeader("Content-Disposition","inline"); 
						  return ok(wwwDir.resolve(index.get()).toFile());
					  } 
				  } //if (!file.isDirectory()) 
			  } //if (fileExists)
		  } //if (BBConfiguration.getInstance().isWWWEnabled())
		  BaasBoxLogger.debug("WWW file not found: " + uri);
		  return redirect(com.baasbox.controllers.routes.MyAssets.at(uri));
	  } //getFile
}
