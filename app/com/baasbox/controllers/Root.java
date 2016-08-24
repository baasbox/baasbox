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
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.ConnectToDBFilterAsync;
import com.baasbox.controllers.actions.filters.RootCredentialWrapFilterAsync;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.OpenTransactionException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.metrics.BaasBoxMetric;
import com.baasbox.service.dbmanager.DbManagerService;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.user.UserService;
import com.baasbox.util.BBJson;
import com.baasbox.util.ErrorToResult;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;


public class Root extends Controller {

	@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> resetAdminPassword(){
		Http.RequestBody body = request().body();

		JsonNode bodyJson= body.asJson();
		if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("resetAdminPassword bodyJson: " + bodyJson);
		//check and validate input
		if (bodyJson==null) {
			return F.Promise.pure(badRequest("The body payload cannot be empty."));
		}
		if (!bodyJson.has("password"))	{
			return F.Promise.pure(badRequest("The 'password' field is missing into the body"));
		}
		JsonNode passwordNode=bodyJson.findValue("password");
		
		if (passwordNode==null) {
			return F.Promise.pure(badRequest("The body payload doesn't contain password field"));
		}
		String password=passwordNode.asText();
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			UserService.changePassword("admin",password);
			return ok("Admin passowrd reset");
		})).recover(
				ErrorToResult.when(SqlInjectionException.class,(e)-> badRequest("The password is not valid"))
							 .when(UserNotFoundException.class,(e)->{
								 String message = "User 'admin' not found!";
								 BaasBoxLogger.error(message);
								 return internalServerError(message);
							 })
							.when(OpenTransactionException.class,
									(e)->{
										BaasBoxLogger.error (ExceptionUtils.getFullStackTrace(e));
										throw new RuntimeException(e);
									}));
	}
	
   	
	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> timers() {
		return returnMetrics(MetricRegistry::getTimers);
	}

	private static F.Promise<Result> returnMetrics(Function<MetricRegistry,SortedMap<String,?>> accessor){
		if (!BaasBoxMetric.isActivate()) {
			return F.Promise.pure(status(SERVICE_UNAVAILABLE, "The metrics service are disabled"));
		}
		try {
			return F.Promise.pure(ok(BBJson.mapper().writeValueAsString(accessor.apply(BaasBoxMetric.registry))));
		} catch (JsonProcessingException e) {
			String msg =e.getMessage();
			return F.Promise.pure(internalServerError(msg==null?"":msg));
		}
	}


	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> counters(){
		return returnMetrics(MetricRegistry::getCounters);
	}

	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> meters() {
		return returnMetrics(MetricRegistry::getMeters);
	}
		
	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> gauges() {
		return returnMetrics(MetricRegistry::getGauges);
	}
		
	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> histograms() {
		return returnMetrics(MetricRegistry::getHistograms);
	}
		
	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> uptime() throws JsonProcessingException {
		HashMap <String,Object> ret = new HashMap<>();
		ret.put("start_time", BaasBoxMetric.Track.getStartTime());
		ret.put("time_zone", "UTC");
		ret.put("uptime", BaasBoxMetric.Track.getUpTimeinMillis());
		ret.put("time_unit", "ms");
		return F.Promise.pure(ok(BBJson.mapper().writeValueAsString(ret)));
	}
		
	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> startMetrics(){
		BaasBoxMetric.start();
		return F.Promise.pure(ok("Metrics service started"));
	}

	@With(RootCredentialWrapFilterAsync.class)
	public static F.Promise<Result> stopMetrics() {
		BaasBoxMetric.stop();
		return F.Promise.pure(ok("Metrics service stopped"));
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
	@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> exportDb(){
		String appcode = (String)ctx().args.get("appcode");

		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			String fileName="";
			try {
				fileName = DbManagerService.exportDb(appcode);
			} catch (FileNotFoundException e) {
				return internalServerError(ExceptionUtils.getMessage(e));
			}
			return status(202, Json.toJson(fileName));
		}));
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
	@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> getExport(String filename){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			java.io.File file = new java.io.File(DbManagerService.backupDir+DbManagerService.fileSeparator+filename);
			if(!file.exists()){
				return notFound();
			}else{
				response().setContentType("application/zip"); //added in Play 2.2.1. it is very strange because the content type should be set by the framework
				return ok(file);
			}
		}));
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
	@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> deleteExport(String fileName){
		return F.Promise.promise(DbHelper.withDbFromContext(ctx(),()->{
			try {
				DbManagerService.deleteExport(fileName);
			} catch (FileNotFoundException e1) {
				return notFound();
			} catch (IOException e1) {
				return internalServerError("Unable to delete export.It will be deleted on the next reboot."+fileName);
			}
			return ok();
		}));
	}
		
	/**
	 * /root/db/export (GET)
	 *
	 * the method returns the list as a json array of all the export files
	 * stored into the db export folder ({@link BBConfiguration#getDBBackupDir()})
	 *
	 * @return a 200 ok code and a json representation containing the list of files stored in the db backup folder
	 */
	@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
	public static F.Promise<Result> getExports(){
		F.Promise<List<String>> asyncFiles = F.Promise.promise(()->{
			try {
				DbHelper.openFromContext(ctx());
				return DbManagerService.getExports();
			}finally {
				DbHelper.close(DbHelper.getConnection());
			}
		});
		return asyncFiles
				.map(Json::toJson)
				.map(Results::ok);
	}
		
		/**
		 * /admin/db/import (POST)
		 * 
		 * the method allows to upload a json export file and apply it to the db.
		 * WARNING: all data on the db will be wiped out before importing
		 * 
		 * @return a 200 Status code when the import is successfull,a 500 status code otherwise
		 */
		@With({RootCredentialWrapFilterAsync.class,ConnectToDBFilterAsync.class})
		public static F.Promise<Result> importDb(){
			String appcode = (String)ctx().args.get("appcode");
			MultipartFormData  body = request().body().asMultipartFormData();
			if (body==null) {
				return F.Promise.pure(badRequest("missing data: is the body multipart/form-data?"));
			}
			FilePart fp = body.getFile("file");
			if (fp == null){
				return F.Promise.pure(badRequest("The form was submitted without a multipart file field."));
			}
			java.io.File multipartFile=fp.getFile();
			java.util.UUID uuid = java.util.UUID.randomUUID();

			return F.Promise.promise(()->{
				java.io.File zipFile = File.createTempFile(uuid.toString(),".zip");
				FileUtils.copyFile(multipartFile,zipFile);
				return zipFile;
			}).<Result>map((zipFile)->{
				DbHelper.openFromContext(ctx());
				try(ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))){
					DbManagerService.importDb(appcode, zis);
					return ok();
				}finally {
					zipFile.delete();
					DbHelper.close(DbHelper.getConnection());
				}
			}).recover((t)->{
				BaasBoxLogger.error(ExceptionUtils.getStackTrace(t));
				return internalServerError(ExceptionUtils.getStackTrace(t));
			});
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
		@With({RootCredentialWrapFilterAsync.class})
		public static F.Promise<Result> overrideConfiguration(){
			Http.RequestBody body = request().body();
			JsonNode bodyJson= body.asJson();
			JsonNode newDBAlert = bodyJson.get(BBConfiguration.getInstance().DB_ALERT_THRESHOLD);
			JsonNode newDBSize = bodyJson.get(BBConfiguration.getInstance().DB_SIZE_THRESHOLD);
			JsonNode enableWWW = bodyJson.get(BBConfiguration.getInstance().WEB_ENABLE);
			
			try{
				if (newDBAlert!=null && !newDBAlert.isInt() && newDBAlert.asInt()<1) {
					throw new IllegalArgumentException(BBConfiguration.getInstance().DB_ALERT_THRESHOLD + " must be a positive integer value");
				}
				if (newDBSize!=null && !newDBSize.isLong() && newDBSize.asInt()<0)	{
					throw new IllegalArgumentException(BBConfiguration.getInstance().DB_SIZE_THRESHOLD + " must be a positive integer value, or 0 to disable it");
				}
				if (enableWWW!=null && !enableWWW.isBoolean())	{
					throw new IllegalArgumentException(BBConfiguration.getInstance().WEB_ENABLE + " must be a boolean");
				}
			}catch (Throwable e){
				return F.Promise.pure(badRequest(ExceptionUtils.getMessage(e)));
			}

			if (newDBAlert!=null){
				BBConfiguration.getInstance().setDBAlertThreshold(newDBAlert.asInt());
			}
			if (newDBSize!=null) {
				BBConfiguration.getInstance().setDBSizeThreshold(BigInteger.valueOf(newDBSize.asLong()));
			}
			if (enableWWW!=null) {
				BBConfiguration.getInstance().setWebEnable(enableWWW.asBoolean());
			}
			
			Map<String, Object> ret = ImmutableMap.of(
					BBConfiguration.getInstance().DB_ALERT_THRESHOLD, BBConfiguration.getInstance().getDBAlertThreshold(),
					BBConfiguration.getInstance().DB_SIZE_THRESHOLD, BBConfiguration.getInstance().getDBSizeThreshold(),
					BBConfiguration.getInstance().WEB_ENABLE, BBConfiguration.getInstance().isWebEnabled()
					);
			try {
				return F.Promise.pure(ok(BBJson.mapper().writeValueAsString(ret)));
			} catch (JsonProcessingException e) {
				return F.Promise.pure(internalServerError(ExceptionUtils.getMessage(e)));
			}
		}
		
		@With({RootCredentialWrapFilterAsync.class})
		public static F.Promise<Result> getOverridableConfiguration(){
			Map<String, ? extends Number> ret = ImmutableMap.of(
					BBConfiguration.getInstance().DB_ALERT_THRESHOLD, BBConfiguration.getInstance().getDBAlertThreshold(),
					BBConfiguration.getInstance().DB_SIZE_THRESHOLD, BBConfiguration.getInstance().getDBSizeThreshold());
			try {
				return F.Promise.pure(ok(BBJson.mapper().writeValueAsString(ret)));
			} catch (JsonProcessingException e) {
				return F.Promise.pure(internalServerError(ExceptionUtils.getMessage(e)));
			}
		}
		
}
