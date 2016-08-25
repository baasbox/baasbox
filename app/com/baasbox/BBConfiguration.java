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
package com.baasbox;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.baasbox.service.logging.BaasBoxLogger;

import play.Configuration;
import play.Play;

public class BBConfiguration implements IBBConfigurationKeys {


	private static BBConfiguration me = null;
	
	public static BBConfiguration getInstance(){
		return me;
	}
	
	public static void shutdown(){
		me=null;
	}
	
	private BBConfiguration(Configuration config){
		configuration = config;
	}
	
	public static void init(){
		me = new BBConfiguration(Play.application().configuration());
	}
	
	public  Configuration configuration;
	
	private  Boolean computeMetrics;
	private  Boolean pushMock;;
	
	//this is a percentage needed by the console to show alerts on dashboard when DB size is near the defined Threshold
	private  Integer dbAlertThreshold=Integer.valueOf(10); 
	private  boolean isDBAlertThresholdOverridden=false; 
	
	//the db size Threshold in bytes
	private  BigInteger dbSizeThreshold=BigInteger.ZERO;
	private  boolean isDBSizeThresholdOverridden=false;

	private Boolean enableWeb = false;;

	private boolean isEnableWebOverridden = false; 
	
	
	@Deprecated
	public  String getRealm(){
		return this.configuration.getString(REALM);
	}
	
	public  int getMVCCMaxRetries(){
		return this.configuration.getInt(MVCC_MAX_RETRIES);
	}
	
	public  String getBaasBoxUsername(){
		return this.configuration.getString(ANONYMOUS_USERNAME);
	}
	
	public  String getBaasBoxPassword(){
		return this.configuration.getString(ANONYMOUS_PASSWORD);
	}
	
	public  String getBaasBoxAdminUsername(){
		return this.configuration.getString(ADMIN_USERNAME);
	}
	
	public  String getBaasBoxAdminPassword(){
		return this.configuration.getString(ADMIN_PASSWORD);
	}

	public   Boolean getStatisticsSystemOS(){
		return this.configuration.getBoolean(STATISTICS_SYSTEM_OS);
	}	

	public  Boolean getStatisticsSystemMemory(){
		return this.configuration.getBoolean(STATISTICS_SYSTEM_MEMORY);
	}

	public  Boolean getWriteAccessLog(){
		return this.configuration.getBoolean(WRITE_ACCESS_LOG);
	}
	
	public  boolean isRedisActive(){
		return this.configuration.getString("redisplugin").equals("enabled");
	}
	
	public  String getApiVersion(){
		return this.configuration.getString(API_VERSION);
	}
	
	public  String getDBVersion(){
		return this.configuration.getString(DB_VERSION);
	}
	
	public  String getDBFullPath(){
		return this.configuration.getString(DB_PATH);
	}
	
	public  String getDBDir(){
		if (getDBFullPath().lastIndexOf(File.separator) > -1)
			return getDBFullPath().substring(0,getDBFullPath().lastIndexOf(File.separator));
		else return getDBFullPath();
	}
	
	public  Boolean getWrapResponse(){
		return Boolean.valueOf(this.configuration.getString(WRAP_RESPONSE));
	}
	
	public  Boolean getSocialMock(){
		return Boolean.valueOf(this.configuration.getString(SOCIAL_MOCK));
	}
	
	public  Boolean getPushMock(){
		if (this.pushMock==null) this.pushMock=	BooleanUtils.isTrue(this.configuration.getBoolean(PUSH_MOCK));
		return this.pushMock;
	}
	
	public  String getAPPCODE() {
		return this.configuration.getString(APP_CODE);
	}
	
	public  String getDBBackupDir() {
		return this.configuration.getString(DB_BACKUP_PATH);
	}
	
	public  String getPushCertificateFolder(){
		return this.configuration.getString(PUSH_CERTIFICATES_FOLDER);
	}

	public  String getRootPassword() {
		return this.configuration.getString(ROOT_PASSWORD);
	}

	public  boolean isRootAsAdmin() {
		Boolean rootAsAdmin=this.configuration.getBoolean(ROOT_AS_ADMIN);
		return rootAsAdmin==null?false:rootAsAdmin.booleanValue();
	}
	
	public  int getImportExportBufferSize(){
		return this.configuration.getInt(DB_IMPORT_EXPORT_BUFFER_SIZE);
	}
	
	public  Boolean isChunkedEnabled(){
		return this.configuration.getBoolean(CHUNKED_RESPONSE);
	}
	
	public  int getChunkSize(){
		return this.configuration.getInt(CHUNK_SIZE);
	}
	
	//sessions
	public  Boolean isSessionEncryptionEnabled(){
		return this.configuration.getBoolean(SESSION_ENCRYPT);
	}
	
	public  String getApplicationSecret(){
		return this.configuration.getString(APPLICATION_SECRET);
	}
	
	public  String getSecretDefault(){
		return "CHANGE_ME";
	}
	
	//metrics
	public  boolean getComputeMetrics() {
		if (this.computeMetrics==null) 
			this.computeMetrics=(!StringUtils.isEmpty(this.configuration.getString(ROOT_PASSWORD)) 
				&& 	BooleanUtils.isTrue(this.configuration.getBoolean(CAPTURE_METRICS)));
		return this.computeMetrics;
	}
	
	public  void overrideConfigurationComputeMetrics(boolean computeMetrics) {
		this.computeMetrics = computeMetrics;
	}
	
	//used by tests
	public  void _overrideConfigurationPushMock(boolean pushMock) {
		this.pushMock = pushMock;
	}
	
	//DB Size Thresholds and Alerts
	public  int getDBAlertThreshold(){
		if (!this.isDBAlertThresholdOverridden && this.configuration.getInt(DB_ALERT_THRESHOLD)!=null) 
			return this.configuration.getInt(DB_ALERT_THRESHOLD);
		return this.dbAlertThreshold;
	}
		
	public  BigInteger getDBSizeThreshold(){
		if (!this.isDBSizeThresholdOverridden && this.configuration.getLong(DB_SIZE_THRESHOLD)!=null) return BigInteger.valueOf(this.configuration.getLong(DB_SIZE_THRESHOLD));
		return this.dbSizeThreshold;
	}
	
	public  void setDBAlertThreshold(int newValue){
		synchronized(this.dbAlertThreshold){
			this.dbAlertThreshold=Integer.valueOf(newValue);
			this.isDBAlertThresholdOverridden=true;
	    }
	}
	
	public  void setDBSizeThreshold(BigInteger newValue){
		synchronized(this.dbSizeThreshold){
			this.dbSizeThreshold=newValue;
			this.isDBSizeThresholdOverridden=true;
	    }
	}
	
	public  Boolean getOrientEnableRemoteConnection() {
		return this.configuration.getBoolean(ORIENT_ENABLE_REMOTE_CONNECTION);
	}

	public  String getOrientListeningPorts() {
		return this.configuration.getString(ORIENT_LISTENING_PORTS);
	}

	public  String getOrientListeningAddress() {
		return this.configuration.getString(ORIENT_LISTENING_ADDRESS);
	}
	
	public  Boolean getOrientStartCluster() {
		return this.configuration.getBoolean(ORIENT_START_CLUSTER);
	}
	
	public String getWebPath(){
		return this.configuration.getString(WEB_PATH);
	}
	
	public List<String> getWebIndexFiles(){
		return this.configuration.getStringList(WEB_INDEX_FILES);	
	}
	
	public boolean isWebEnabled(){
		if (!this.isEnableWebOverridden && this.configuration.getBoolean(WEB_ENABLE)!=null) {
			return this.configuration.getBoolean(WEB_ENABLE);
		} 
		return this.enableWeb;
	}

	public void setWebEnable(boolean newValue) {
		synchronized(this.enableWeb){
			this.enableWeb=newValue;
			this.isEnableWebOverridden=true;
	    }
		if (newValue){
			BaasBoxLogger.info("Static Web Service has been enabled");
			BaasBoxLogger.info("WWW folder is " + getWebAbsolutePath());
		} else {
			BaasBoxLogger.info("Static Web Service has been disabled");
		}
	}

	public String getWebAbsolutePath() {
		return Paths.get(getWebPath()).toFile().getAbsolutePath();
	}
	
}
