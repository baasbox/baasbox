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

package com.baasbox.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilter;
import com.baasbox.controllers.actions.filters.RootCredentialWrapFilter;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.metrics.BaasBoxMetric;
import com.baasbox.service.dbmanager.DbManagerService;
import com.baasbox.service.user.UserService;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class Root extends Controller {

	@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
	public static Result resetAdminPassword(){
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (Logger.isDebugEnabled()) Logger.debug("resetAdminPassword bodyJson: " + bodyJson);
		//check and validate input
		if (bodyJson==null) return badRequest("The body payload cannot be empty.");		
		if (!bodyJson.has("password"))	return badRequest("The 'password' field is missing into the body");
		JsonNode passwordNode=bodyJson.findValue("password");
		
		if (passwordNode==null) return badRequest("The body payload doesn't contain password field");
		String password=passwordNode.asText();	 
		try{
			UserService.changePassword("admin", password);
		} catch (SqlInjectionException e) {
			return badRequest("The password is not valid");
		} catch (UserNotFoundException e) {
			Logger.error("User 'admin' not found!");
		    return internalServerError("User 'admin' not found!");
		} catch (OpenTransactionException e) {
			Logger.error (ExceptionUtils.getFullStackTrace(e));
			throw new RuntimeException(e);
		}
		return ok("Admin password reset");
	}
	
   	
		@With(RootCredentialWrapFilter.class)
		public static Result timers() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
			ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
			return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getTimers()));
	    }

		@With(RootCredentialWrapFilter.class)
	    public static Result counters() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getCounters()));
	    }

		@With(RootCredentialWrapFilter.class)
	    public static Result meters() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getMeters()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result gauges() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service are disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getGauges()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result histograms() throws JsonProcessingException {
			if (!BaasBoxMetric.isActivate()) return status(SERVICE_UNAVAILABLE,"The metrics service is disabled");
	    	ObjectMapper mapper = new ObjectMapper().registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
	        return ok(mapper.writeValueAsString(BaasBoxMetric.registry.getHistograms()));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result uptime() throws JsonProcessingException {
	    	ObjectMapper mapper = new ObjectMapper();
	    	HashMap <String,Object> ret = new HashMap<String, Object>();
	    	ret.put("start_time", BaasBoxMetric.Track.getStartTime());
	    	ret.put("time_zone", "UTC");
	    	ret.put("uptime", BaasBoxMetric.Track.getUpTimeinMillis());
	    	ret.put("time_unit", "ms");
	        return ok(mapper.writeValueAsString(ret));
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result startMetrics() throws JsonProcessingException {
			BaasBoxMetric.start();
	        return ok("Metrics service started");
	    }
		
		@With(RootCredentialWrapFilter.class)
	    public static Result stopMetrics() throws JsonProcessingException {
			BaasBoxMetric.stop();
	        return ok("Metrics service stopped");
	    }
		
		//backup & restore
		/**
		 * /root/db/export (POST)
		 * 
		 * the method generate a full dump of the db in an asyncronus task.
		 * the response returns a 202 code (ACCEPTED) and the filename of the
		 * file that will be generated.
		 * 
		 * The async nature of the method DOES NOT ensure the creation of the file
		 * so, querying for the file name with the /admin/db/:filename could return a 404
		 * @return a 202 accepted code and a json representation containing the filename of the generated file
		 */
		@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
		public static Result exportDb(){
			String appcode = (String)ctx().args.get("appcode");
			String fileName="";
			try {
				fileName = DbManagerService.exportDb(appcode);
			} catch (FileNotFoundException e) {
				return internalServerError(e.getMessage());
			}
			return status(202,Json.toJson(fileName));
		}
		
		/**
		 * /root/db/export/:filename (GET)
		 * 
		 * the method returns the stream of the export file named after :filename parameter. 
		 * 
		 * if the file is not present a 404 code is returned to the client
		 * 
		 * @return a 200 ok code and the stream of the file
		 */
		@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
		public static Result getExport(String filename){
			java.io.File file = new java.io.File(DbManagerService.backupDir+DbManagerService.fileSeparator+filename);
			if(!file.exists()){
				return notFound();
			}else{
				response().setContentType("application/zip"); //added in Play 2.2.1. it is very strange because the content type should be set by the framework
				return ok(file);
			}

		}
		
		/**
		 * /root/db/export/:filename (DELETE)
		 * 
		 * Deletes an export file from the db backup folder, if it exists 
		 * 
		 * 
		 * @param fileName the name of the file to be deleted
		 * @return a 200 code if the file is deleted correctly or a 404.If the file could not
		 * be deleted a 500 error code is returned
		 */
		@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
		public static Result deleteExport(String filename){
			try {
				DbManagerService.deleteExport(filename);
			} catch (FileNotFoundException e1) {
				return notFound();
			} catch (IOException e1) {
				return internalServerError("Unable to delete export.It will be deleted on the next reboot."+filename);
			}
			return ok();
		}
		
		/**
		 * /root/db/export (GET)
		 * 
		 * the method returns the list as a json array of all the export files
		 * stored into the db export folder ({@link BBConfiguration#getDBBackupDir()})
		 * 
		 * @return a 200 ok code and a json representation containing the list of files stored in the db backup folder
		 */
		@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
		public static Result getExports(){
			List<String> fileNames = DbManagerService.getExports();
			return ok(Json.toJson(fileNames));
		}
		
		/**
		 * /admin/db/import (POST)
		 * 
		 * the method allows to upload a json export file and apply it to the db.
		 * WARNING: all data on the db will be wiped out before importing
		 * 
		 * @return a 200 Status code when the import is successfull,a 500 status code otherwise
		 */
		@With({RootCredentialWrapFilter.class,ConnectToDBFilter.class})
		public static Result importDb(){
			String appcode = (String)ctx().args.get("appcode");
			MultipartFormData  body = request().body().asMultipartFormData();
			if (body==null) return badRequest("missing data: is the body multipart/form-data?");
			FilePart fp = body.getFile("file");

			if (fp!=null){
				ZipInputStream zis = null;
				try{
					java.io.File multipartFile=fp.getFile();
					java.util.UUID uuid = java.util.UUID.randomUUID();
					File zipFile = File.createTempFile(uuid.toString(), ".zip");
					FileUtils.copyFile(multipartFile,zipFile);
					zis = 	new ZipInputStream(new FileInputStream(zipFile));
					DbManagerService.importDb(appcode, zis);
					zipFile.delete();
					return ok();		
				}catch(Exception e){
					Logger.error(ExceptionUtils.getStackTrace(e));
					return internalServerError(ExceptionUtils.getStackTrace(e));
				}finally{
					try {
						if(zis!=null){
							zis.close();
						}
					} catch (IOException e) {
						// Nothing to do here
					}
				}
			}else{
				return badRequest("The form was submitted without a multipart file field.");
			}
		}
		
		//DB size and alert thresholds
		/**
		 * /root/configuration (POST)
		 * 
		 * this method allows to set (or override) just two configuration parameter (at the moment)
		 * the db size Threshold in bytes:
		 * 		baasbox.db.size 
		 * A percentage needed by the console to show alerts on dashboard when DB size is near the defined Threshold
		 * 		baasbox.db.alert
		 * 
		 * @return a 200 OK with the new values
		 */
		@With({RootCredentialWrapFilter.class})
		public static Result overrideConfiguration(){
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			JsonNode newDBAlert = bodyJson.get(BBConfiguration.DB_ALERT_THRESHOLD);
			JsonNode newDBSize = bodyJson.get(BBConfiguration.DB_SIZE_THRESHOLD);
			try{
				if (newDBAlert!=null && !newDBAlert.isInt() && newDBAlert.asInt()<1) throw new IllegalArgumentException(BBConfiguration.DB_ALERT_THRESHOLD + " must be a positive integer value");
				if (newDBSize!=null && !newDBSize.isLong() && newDBSize.asInt()<0) throw new IllegalArgumentException(BBConfiguration.DB_SIZE_THRESHOLD + " must be a positive integer value, or 0 to disable it");
			}catch (Throwable e){
				return badRequest(e.getMessage());
			}
			if (newDBAlert!=null) BBConfiguration.setDBAlertThreshold(newDBAlert.asInt());
			if (newDBSize!=null) BBConfiguration.setDBSizeThreshold(BigInteger.valueOf(newDBSize.asLong()));
			HashMap returnMap = new HashMap();
			returnMap.put(BBConfiguration.DB_ALERT_THRESHOLD, BBConfiguration.getDBAlertThreshold());
			returnMap.put(BBConfiguration.DB_SIZE_THRESHOLD, BBConfiguration.getDBSizeThreshold());
			try {
				return ok(new ObjectMapper().writeValueAsString(returnMap));
			} catch (JsonProcessingException e) {
				return internalServerError(e.getMessage());
			}
		}
		
		@With({RootCredentialWrapFilter.class})
		public static Result getOverridableConfiguration(){
			HashMap returnMap = new HashMap();
			returnMap.put(BBConfiguration.DB_ALERT_THRESHOLD, BBConfiguration.getDBAlertThreshold());
			returnMap.put(BBConfiguration.DB_SIZE_THRESHOLD, BBConfiguration.getDBSizeThreshold());
			try {
				return ok(new ObjectMapper().writeValueAsString(returnMap));
			} catch (JsonProcessingException e) {
				return internalServerError(e.getMessage());
			}
		}
		
}
