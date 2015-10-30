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

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import play.Configuration;

public class BBConfiguration implements IBBConfigurationKeys {

	private static BBConfiguration me = null;
	
	private BBConfiguration(Configuration config){
		configuration = config;
	}
	
	public static void init(Configuration config){
		if (me==null){
			System.out.println("Configuration initialized");
			me=new BBConfiguration(config);
		}
	}
	
	public static Configuration configuration;
	private static Boolean computeMetrics;
	private static Boolean pushMock;;
	
	//this is a percentage needed by the console to show alerts on dashboard when DB size is near the defined Threshold
	private static Integer dbAlertThreshold=Integer.valueOf(10); 
	private static boolean isDBAlertThresholdOverridden=false; 
	
	//the db size Threshold in bytes
	private static BigInteger dbSizeThreshold=BigInteger.ZERO;
	private static boolean isDBSizeThresholdOverridden=false; 
	
	
	@Deprecated
	public static String getRealm(){
		return configuration.getString(REALM);
	}
	
	public static int getMVCCMaxRetries(){
		return configuration.getInt(MVCC_MAX_RETRIES);
	}
	
	public static String getBaasBoxUsername(){
		return configuration.getString(ANONYMOUS_USERNAME);
	}
	
	public static String getBaasBoxPassword(){
		return configuration.getString(ANONYMOUS_PASSWORD);
	}
	
	public static String getBaasBoxAdminUsername(){
		return configuration.getString(ADMIN_USERNAME);
	}
	
	public static String getBaasBoxAdminPassword(){
		return configuration.getString(ADMIN_PASSWORD);
	}

	public static  Boolean getStatisticsSystemOS(){
		return configuration.getBoolean(STATISTICS_SYSTEM_OS);
	}	

	public static Boolean getStatisticsSystemMemory(){
		return configuration.getBoolean(STATISTICS_SYSTEM_MEMORY);
	}

	public static Boolean getWriteAccessLog(){
		return configuration.getBoolean(WRITE_ACCESS_LOG);
	}
	
	public static boolean isRedisActive(){
		return BBConfiguration.configuration.getString("redisplugin").equals("enabled");
	}
	
	public static String getApiVersion(){
		return configuration.getString(API_VERSION);
	}
	
	public static String getDBVersion(){
		return configuration.getString(DB_VERSION);
	}
	
	public static String getDBFullPath(){
		return configuration.getString(DB_PATH);
	}
	
	public static String getDBDir(){
		if (getDBFullPath().lastIndexOf(File.separator) > -1)
			return getDBFullPath().substring(0,getDBFullPath().lastIndexOf(File.separator));
		else return getDBFullPath();
	}
	
	public static Boolean getWrapResponse(){
		return Boolean.valueOf(configuration.getString(WRAP_RESPONSE));
	}
	
	public static Boolean getSocialMock(){
		return Boolean.valueOf(configuration.getString(SOCIAL_MOCK));
	}
	
	public static Boolean getPushMock(){
		if (pushMock==null) pushMock=	BooleanUtils.isTrue(configuration.getBoolean(PUSH_MOCK));
		return pushMock;
	}
	
	public static String getAPPCODE() {
		return configuration.getString(APP_CODE);
	}
	
	public static String getDBBackupDir() {
		return configuration.getString(DB_BACKUP_PATH);
	}
	
	public static String getPushCertificateFolder(){
		return configuration.getString(PUSH_CERTIFICATES_FOLDER);
	}

	public static String getRootPassword() {
		return configuration.getString(ROOT_PASSWORD);
	}

	public static boolean isRootAsAdmin() {
		Boolean rootAsAdmin=configuration.getBoolean(ROOT_AS_ADMIN);
		return rootAsAdmin==null?false:rootAsAdmin.booleanValue();
	}
	
	public static int getImportExportBufferSize(){
		return configuration.getInt(DB_IMPORT_EXPORT_BUFFER_SIZE);
	}
	
	public static Boolean isChunkedEnabled(){
		return configuration.getBoolean(CHUNKED_RESPONSE);
	}
	
	public static int getChunkSize(){
		return configuration.getInt(CHUNK_SIZE);
	}
	
	//sessions
	public static Boolean isSessionEncryptionEnabled(){
		return configuration.getBoolean(SESSION_ENCRYPT);
	}
	
	public static String getApplicationSecret(){
		return configuration.getString(APPLICATION_SECRET);
	}
	
	public static String getSecretDefault(){
		return "CHANGE_ME";
	}
	
	//metrics
	public static boolean getComputeMetrics() {
		if (computeMetrics==null) 
			computeMetrics=(!StringUtils.isEmpty(configuration.getString(ROOT_PASSWORD)) 
				&& 	BooleanUtils.isTrue(configuration.getBoolean(CAPTURE_METRICS)));
		return computeMetrics;
	}
	
	public static void overrideConfigurationComputeMetrics(boolean computeMetrics) {
		BBConfiguration.computeMetrics = computeMetrics;
	}
	
	//used by tests
	public static void _overrideConfigurationPushMock(boolean pushMock) {
		BBConfiguration.pushMock = pushMock;
	}
	
	//DB Size Thresholds and Alerts
	public static int getDBAlertThreshold(){
		if (!isDBAlertThresholdOverridden && configuration.getInt(DB_ALERT_THRESHOLD)!=null) return configuration.getInt(DB_ALERT_THRESHOLD);
		return dbAlertThreshold;
	}
	
	public static BigInteger getDBSizeThreshold(){
		if (!isDBSizeThresholdOverridden && configuration.getLong(DB_SIZE_THRESHOLD)!=null) return BigInteger.valueOf(configuration.getLong(DB_SIZE_THRESHOLD));
		return dbSizeThreshold;
	}
	
	public static void setDBAlertThreshold(int newValue){
		synchronized(dbAlertThreshold){
			dbAlertThreshold=Integer.valueOf(newValue);
			isDBAlertThresholdOverridden=true;
	    }
	}
	
	public static void setDBSizeThreshold(BigInteger newValue){
		synchronized(dbSizeThreshold){
			dbSizeThreshold=newValue;
			isDBSizeThresholdOverridden=true;
	    }
	}
	
	public static Boolean getOrientEnableRemoteConnection() {
		return configuration.getBoolean(ORIENT_ENABLE_REMOTE_CONNECTION);
	}

	public static String getOrientListeningPorts() {
		return configuration.getString(ORIENT_LISTENING_PORTS);
	}

	public static String getOrientListeningAddress() {
		return configuration.getString(ORIENT_LISTENING_ADDRESS);
	}
	
	public static Boolean getOrientStartCluster() {
		return configuration.getBoolean(ORIENT_START_CLUSTER);
	}
	
	
}
