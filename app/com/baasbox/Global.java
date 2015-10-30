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

import static play.Logger.debug;
import static play.Logger.error;
import static play.Logger.info;
import static play.Logger.warn;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.math.NumberUtils;

import play.Application;
import play.Configuration;
import play.GlobalSettings;
import play.Play;
import play.api.mvc.EssentialFilter;
import play.core.j.JavaResultExtractor;
import play.filters.gzip.GzipFilter;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.RequestHeader;
import play.mvc.SimpleResult;

import com.baasbox.configuration.Internal;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.configuration.PropertiesConfigurationHelper;
import com.baasbox.db.DbHelper;
import com.baasbox.metrics.BaasBoxMetric;
import com.baasbox.security.ISessionTokenProvider;
import com.baasbox.security.ScriptingSandboxSecurityManager;
import com.baasbox.security.SessionTokenProviderFactory;
import com.baasbox.security.SessionTokenProviderMemory;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.service.storage.StatisticsService;
import com.baasbox.util.BBJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

public class Global extends GlobalSettings {
	static {
        /*Initialize this before anything else to avoid reflection*/
        ScriptingSandboxSecurityManager.init();
    }

	  private static Boolean  justCreated = false;
	  private static OServer server = null;

	@Override
	  public void beforeStart(Application app) {
		  info("BaasBox is starting...");
		  info("...Loading Play plugin...");
		  System.out.println("SOCIAL from BBConfiguration: "+BBConfiguration.getSocialMock());
		  System.out.println("SOCIAL from app: "+app.configuration().getBoolean("baasbox.social.mock"));
		  
			
		  //If the session encryption is enabled, checks if the secret is different by its default
		  if (app.configuration().getBoolean(BBConfiguration.SESSION_ENCRYPT)
				  &&
			  app.configuration().getString(BBConfiguration.APPLICATION_SECRET).equals(BBConfiguration.getSecretDefault())){
				  error("The session encryption is enabled but the application secret has not been changed.");
				  error("Please set the 'application.secret' parameter. It must be a string of 16 characters.");
				  error("For security reasons BaasBox cannot start");
				  System.exit(-1);
			  }
		  
		  info("System details:");
		  info(StatisticsService.os().toString());
		  info(StatisticsService.memory().toString());
		  info(StatisticsService.java().toString());
		  if (Boolean.parseBoolean(app.configuration().getString(BBConfiguration.DUMP_DB_CONFIGURATION_ON_STARTUP))) info(StatisticsService.db().toString()); 
	  }
	  
	  @Override
	  public Configuration onLoadConfig(Configuration config,
          java.io.File path,
          java.lang.ClassLoader classloader){  
		  BBConfiguration.init(config);
		  debug("Global.onLoadConfig() called");
		  info("BaasBox is preparing OrientDB Embedded Server...");
		  
		  
		  System.out.println(String.format("process id: %s thread id: %s", ManagementFactory.getRuntimeMXBean()
				    .getName(), Thread.currentThread().getId()));
		  System.out.println("SOCIAL METHOD from configuration: "+config.getBoolean("baasbox.social.mock"));
		  System.out.println("configuration: "+config.toString());
		 
		  
		  
		  
		  try{
			  OGlobalConfiguration.TX_LOG_SYNCH.setValue(Boolean.FALSE);
			  OGlobalConfiguration.TX_COMMIT_SYNCH.setValue(Boolean.FALSE);
			  
			  OGlobalConfiguration.NON_TX_RECORD_UPDATE_SYNCH.setValue(Boolean.FALSE);
			  //Deprecated due to OrientDB 1.6
			  //OGlobalConfiguration.NON_TX_CLUSTERS_SYNC_IMMEDIATELY.setValue(OMetadata.CLUSTER_MANUAL_INDEX_NAME);
			  
			  OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(Boolean.FALSE);
			  OGlobalConfiguration.CACHE_LEVEL2_ENABLED.setValue(Boolean.TRUE);
			  
			  OGlobalConfiguration.INDEX_MANUAL_LAZY_UPDATES.setValue(-1);
			  OGlobalConfiguration.FILE_LOCK.setValue(false);
			  
			  OGlobalConfiguration.FILE_DEFRAG_STRATEGY.setValue(1);
			  
			  OGlobalConfiguration.MEMORY_USE_UNSAFE.setValue(false);

			  if (!NumberUtils.isNumber(System.getProperty("storage.wal.maxSize"))) OGlobalConfiguration.WAL_MAX_SIZE.setValue(1000);
			  
			  if (NumberUtils.isNumber(System.getProperty("db.pool.max"))) {
				  OGlobalConfiguration.DB_POOL_MAX.setValue(System.getProperty("db.pool.max"));
			  } else {
				  OGlobalConfiguration.DB_POOL_MAX.setValue(config.getString("akka.actor.default-dispatcher.fork-join-executor.parallelism-max"));
			  }
			  
			  Orient.instance().startup();
			  ODatabaseDocumentTx db = null;
			  try{
				db =  Orient.instance().getDatabaseFactory().createDatabase("graph", "plocal:" + config.getString(BBConfiguration.DB_PATH) );
				if (!db.exists()) {
					info("DB does not exist, BaasBox will create a new one");
					db.create();
					justCreated  = true;
                    info("DB has been create successfully");
                   
				}
			  } catch (Throwable e) {
					error("!! Error initializing BaasBox!", e);
					error(ExceptionUtils.getFullStackTrace(e));
					throw e;
			  } finally {
		    	 if (db!=null && !db.isClosed()) db.close();
			  }
		    }catch (Throwable e){
		    	error("!! Error initializing BaasBox!", e);
		    	error("Abnormal BaasBox termination.");
		    	System.exit(-1);
		    }
		  info("Max DB connections: {}",OGlobalConfiguration.DB_POOL_MAX.getValueAsInteger());
		  debug("Global.onLoadConfig() ended");
		  return config;
	  }
	  
	  @Override
	  public void onStart(Application app) {
		 debug("Global.onStart() called");
		 
	    ODatabaseRecordTx db =null;
	    try{
	    	createOrientDBDeamon();
	    	if (justCreated){
		    	try {
		    		//we MUST use admin/admin because the db was just created
		    		db = DbHelper.open( BBConfiguration.getAPPCODE(),"admin", "admin");
		    		DbHelper.setupDb();
		    	}catch (Throwable e){
					error("!! Error initializing BaasBox!", e);
					error(ExceptionUtils.getFullStackTrace(e));
					throw e;
		    	} finally {
		    		if (db!=null && !db.isClosed()) db.close();
		    	}
		    	justCreated=false;
	    	}
	    }catch (Throwable e){
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
	    }
    	info("Updating default users passwords...");
    	try {
    		db = DbHelper.open( BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminPassword());
    		DbHelper.evolveDB(db);
			DbHelper.updateDefaultUsers();
			info("Initializing session manager");
	    	ISessionTokenProvider stp = SessionTokenProviderFactory.getSessionTokenProvider();
	    	stp.setTimeout(com.baasbox.configuration.Application.SESSION_TOKENS_TIMEOUT.getValueAsInteger()*60*1000); //minutes * 60 seconds * 1000 milliseconds
	    	
			String bbid=Internal.INSTALLATION_ID.getValueAsString();
			if (bbid==null) throw new Exception ("Unique id not found! Hint: could the DB be corrupted?");
			info ("BaasBox unique id is " + bbid);
		} catch (Exception e) {
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
		} finally {
    		if (db!=null && !db.isClosed()) db.close();
    	}
    	
    	try{
    		db = DbHelper.open( BBConfiguration.getAPPCODE(), BBConfiguration.getBaasBoxAdminUsername(), BBConfiguration.getBaasBoxAdminPassword());
    		IosCertificateHandler.init();
    	}catch (Exception e) {
	    	error("!! Error initializing BaasBox!", e);
	    	error("Abnormal BaasBox termination.");
	    	System.exit(-1);
		} finally {
    		if (db!=null && !db.isClosed()) db.close();
    	}
    	info ("...done");
    	
    	overrideSettings();
    	
    	//activate metrics
    	BaasBoxMetric.setExcludeURIStartsWith(com.baasbox.controllers.routes.Root.startMetrics().url());
    	if (BBConfiguration.getComputeMetrics()) BaasBoxMetric.start();
    	
    	//print out Redis info
    	if (BBConfiguration.isRedisActive()){
    		info("BaasBox will use REDIS as external cache");
    		String redisHost=BBConfiguration.configuration.getString("redis.host");
    		String redisPort=BBConfiguration.configuration.getString("redis.port");
    		String redisURI=BBConfiguration.configuration.getString("redis.uri");
    		String redisDatabase=BBConfiguration.configuration.getString("redis.database");
    		if (StringUtils.isBlank(redisHost)) redisHost="localhost";
    		if (StringUtils.isBlank(redisPort)) redisPort="6379";
    		if (StringUtils.isBlank(redisDatabase)) redisDatabase="0";
    		info("REDIS server: " + (StringUtils.isBlank(redisURI)?redisHost + ":" + redisPort:redisURI));
    		info("REDIS database: " + redisDatabase);
    	}
    	//print out Sessions encryption info
    	if (BBConfiguration.isSessionEncryptionEnabled()){
    		info("BaasBox will encrypt sensitive information within users' sessions");
    		if (BBConfiguration.getApplicationSecret().length()<16){
    			error("The encription key has less than 16 character. Please check the application.secret parameter");
    			System.exit(-1);
    		}
    		if (BBConfiguration.getApplicationSecret().length()>16){
    			warn("The encription key has more than 16 character. Only 16 characters will be used (128 bits)");
    		}
    		if (!BBConfiguration.isRedisActive()){
    			warn("REDIS is not enabled: encryption will not work in memory. Check and set 'redisplugin' parameter");
    		}
    	}
    	
    	//prepare the Welcome Message
	    String port=Play.application().configuration().getString("http.port");
	    if (port==null) port="9000";
	    String address=Play.application().configuration().getString("http.address");
	    if (address==null) address="localhost";
	    
	    //write the Welcome Message
	    info("");
	    info("To login into the administration console go to http://" + address +":" + port + "/console");
	    info("Default credentials are: user:admin pass:admin (if you did not changed it) AppCode: " + BBConfiguration.getAPPCODE());
	    info("Documentation is available at http://www.baasbox.com/documentation");
		debug("Global.onStart() ended");
	    info("BaasBox is Ready.");
	  }

	private void createOrientDBDeamon() throws Exception,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException, InvocationTargetException,
			NoSuchMethodException, IOException {
		if (BBConfiguration.getOrientEnableRemoteConnection() || BBConfiguration.getOrientStartCluster()){
			if (BBConfiguration.configuration.getBoolean(BBConfiguration.DUMP_DB_CONFIGURATION_ON_STARTUP)){
				BaasBoxLogger.info("*** DUMP of OrientDB daemon configuration: ");
				BaasBoxLogger.info(getOrientConfString());
			}
			BaasBoxLogger.info("Starting OrientDB daemon...");
			server = OServerMain.create();
			String daemonConf=getOrientConfString();
			server.startup(daemonConf);
			server.activate();
			server.getNetworkListeners().stream().forEach(x->{
				BaasBoxLogger.info("OrientDB daemon is listening on {}",x.getListeningAddress(true));
			});
			BaasBoxLogger.info("...done");
		}
	}

	private String getOrientConfString() {
		Path currentRelativePath = Paths.get("");
		Path dbPath=currentRelativePath.resolve(BBConfiguration.getDBDir());
		System.setProperty("ORIENTDB_HOME",currentRelativePath.toAbsolutePath().toString());
		String toReturn=
 			   "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
	    			   + "<orient-server>"
	    			   + " <handlers>"
	    			   + (BBConfiguration.getOrientStartCluster()?
		    			     " <handler class=\"com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin\">"
		    			   + " <parameters>"
		    	           + "     <parameter name=\"nodeName\" value=\""+UUID.randomUUID()+"\" /> "
		    	           + "     <parameter name=\"enabled\" value=\"true\"/>"
		    	           + "     <parameter name=\"configuration.db.default\""
		    	           //+ "                value=\"/Users/geniusatwork/Documents/git/giastfader/baasbox/conf/default-distributed-db-config.json\"/>"
		    	           + "                value=\"conf/default-distributed-db-config.json\"/>"
		    	           + "     <parameter name=\"configuration.hazelcast\" "
		    	           //+ "				  value=\"/Users/geniusatwork/Documents/git/giastfader/baasbox/conf/hazelcast.xml\"/>"
		    	           + "				  value=\"conf/hazelcast.xml\"/>"
		    	           + "     <parameter name=\"conflict.resolver.impl\""
		    	           + "                value=\"com.orientechnologies.orient.server.distributed.conflict.ODefaultReplicationConflictResolver\"/>"
	
		    	           + "     <!-- PARTITIONING STRATEGIES -->"
		    	           + "     <parameter name=\"sharding.strategy.round-robin\""
		    	           + "                value=\"com.orientechnologies.orient.server.hazelcast.sharding.strategy.ORoundRobinPartitioninStrategy\"/>"
		    	           + " </parameters>"
		    	           + " </handler>"
	    	           :"")
	    	           + " </handlers>"
		
	    			   + "<network>"
	    			   + "<protocols>"
	    			   + (BBConfiguration.getOrientEnableRemoteConnection()?"<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>":"")
	    			   + "</protocols>"
	    			   + "<listeners>"
					   + (BBConfiguration.getOrientEnableRemoteConnection()?"<listener ip-address=\""+BBConfiguration.getOrientListeningAddress()+"\" port-range=\""+BBConfiguration.getOrientListeningPorts()+"\" protocol=\"binary\"/>":"")
					   + "</listeners>"
	    			   + "</network>"
	    			   + "<users>"
	    			   + "<user name=\"root\" password=\""+(StringUtils.isEmpty(BBConfiguration.getRootPassword()) ? UUID.randomUUID().toString():BBConfiguration.getRootPassword())+"\" resources=\"*\"/>"
	    			   + "</users>"
	    			   + "<properties>"
	    			   //+ (BBConfiguration.getOrientStartCluster() ?  "<entry name=\"cache.level2.impl\" value=\"com.orientechnologies.orient.server.hazelcast.OHazelcastCache\" />"
	    			   //:"")
	    			  // + "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
	    			   + "<entry name=\"log.console.level\" value=\"WARNING\"/>"
	    			   + "<entry name=\"log.file.level\" value=\"WARNING\"/>"
	    			   // + "<entry value=\""+BBConfiguration.getDBDir()+"\" name=\"server.database.path\" />"
	    			  // + "<entry name=\"server.database.path\"  "
	    			  // + "		 value=\"" + BBConfiguration.getDBFullPath() + "\"/>"
	    			  // + "		 value=\"" + dbPath.toAbsolutePath().toString() + "/\"/>"
	    			 
	    			   //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
	    			   + "<entry name=\"plugin.dynamic\" value=\"false\"/>"
	    			   + "</properties>" + 
	    			   "</orient-server>";
		return toReturn;
	}

	private void overrideSettings() {
		info ("Override settings...");
    	//takes only the settings that begin with baasbox.settings
    	Configuration bbSettingsToOverride=BBConfiguration.configuration.getConfig("baasbox.settings");
    	//if there is at least one of them
    	if (bbSettingsToOverride!=null) {
    		//takes the part after the "baasbox.settings" of the key names
    		Set<String> keys = bbSettingsToOverride.keys();
    		Iterator<String> keysIt = keys.iterator();
    		//for each setting to override
    		while (keysIt.hasNext()){
    			String key = keysIt.next();
    			//is it a value to override?
    			if (key.endsWith(".value")){
    				//sets the overridden value
    				String value = "";
    				try {
     					value = bbSettingsToOverride.getString(key);
    					key = key.substring(0, key.lastIndexOf(".value"));
						PropertiesConfigurationHelper.override(key,value);
					} catch (Exception e) {
                        error ("Error overriding the setting " + key + " with the value " + value + ": " +ExceptionUtils.getMessage(e));
					}
    			}else if (key.endsWith(".visible")){ //or maybe we have to hide it when a REST API is called
    				//sets the visibility
    				Boolean value;
    				try {
     					value = bbSettingsToOverride.getBoolean(key);
    					key = key.substring(0, key.lastIndexOf(".visible"));
						PropertiesConfigurationHelper.setVisible(key,value);
					} catch (Exception e) {
						error ("Error overriding the visible attribute for setting " + key + ": " +ExceptionUtils.getMessage(e));
					}
    			}else if (key.endsWith(".editable")){ //or maybe we have to 
    				//sets the possibility to edit the value via REST API by the admin
    				Boolean value;
    				try {
     					value = bbSettingsToOverride.getBoolean(key);
    					key = key.substring(0, key.lastIndexOf(".editable"));
						PropertiesConfigurationHelper.setEditable(key,value);
					} catch (Exception e) {
						error ("Error overriding the editable attribute setting " + key + ": " +ExceptionUtils.getMessage(e));
					}
    			}else { 
    				error("The configuration key: " + key + " is invalid. value, visible or editable are missing");
    			}
    			key.subSequence(0, key.lastIndexOf("."));
    		}
    	}else info ("...No setting to override...");
    	info ("...done");
	}
	  
	  
	  
	  @Override
	  public void onStop(Application app) {
		debug("Global.onStop() called");
	    info("BaasBox is shutting down...");
	    try{
	    	info("Closing the DB connections...");
	    	ODatabaseDocumentPool.global().close();
	    	info("Shutting down embedded OrientDB Server");
	    	//Orient.instance().shutdown();
	    	if (server!=null) server.shutdown();
	    	info("...ok");
	    }catch (ODatabaseException e){
	    	error("Error closing the DB!",e);
	    }catch (Throwable e){
	    	error("!! Error shutting down BaasBox!", e);
	    }
	    info("Destroying session manager...");
	    SessionTokenProviderMemory.destroySessionTokenProvider();
	    info("...BaasBox has stopped");
		debug("Global.onStop() ended");
	  }  
	  
	private void setCallIdOnResult(RequestHeader request, ObjectNode result) {
		String callId = request.getQueryString("call_id");
		if (!StringUtils.isEmpty(callId)) result.put("call_id",callId);
	}
	
	public ObjectNode prepareError(RequestHeader request, String error) {
		ObjectNode result = Json.newObject();
		ObjectMapper mapper = BBJson.mapper();
			result.put("result", "error");
			result.put("message", error);
			result.put("resource", request.path());
			result.put("method", request.method());
			result.put("request_header", (JsonNode)mapper.valueToTree(request.headers()));
			result.put("API_version", BBConfiguration.configuration.getString(BBConfiguration.API_VERSION));
			setCallIdOnResult(request, result);
		return result;
	} 
		
	  @Override
	  public F.Promise<SimpleResult> onBadRequest(RequestHeader request, String error) {
		  ObjectNode result = prepareError(request, error);
		  result.put("http_code", 400);
		  SimpleResult resultToReturn =  badRequest(result);
		  try {
			if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + result.toString(),"UTF-8");
		  }finally{
			  return F.Promise.pure (resultToReturn);
		  }
	  }  

	// 404
	  @Override
	    public F.Promise<SimpleResult> onHandlerNotFound(RequestHeader request) {
		  debug("API not found: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, "API not found");
		  result.put("http_code", 404);
		  SimpleResult resultToReturn= notFound(result);
		  try {
			  if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(resultToReturn),"UTF-8"));
		  }finally{
			  return F.Promise.pure (resultToReturn);
		  }
	    }

	  // 500 - internal server error
	  @Override
	  public F.Promise<SimpleResult> onError(RequestHeader request, Throwable throwable) {
		  error("INTERNAL SERVER ERROR: " + request.method() + " " + request);
		  ObjectNode result = prepareError(request, ExceptionUtils.getMessage(throwable));
		  result.put("http_code", 500);
		  result.put("stacktrace", ExceptionUtils.getFullStackTrace(throwable));
		  error(ExceptionUtils.getFullStackTrace(throwable));
		  SimpleResult resultToReturn= internalServerError(result);
		  try {
			  if (BaasBoxLogger.isDebugEnabled()) BaasBoxLogger.debug("Global.onBadRequest:\n  + result: \n" + result.toString() + "\n  --> Body:\n" + new String(JavaResultExtractor.getBody(resultToReturn),"UTF-8"));
		  } finally{
			  return F.Promise.pure (resultToReturn);
		  }
	  }


	@Override 
	public <T extends EssentialFilter> Class<T>[] filters() {
		return new Class[]{GzipFilter.class,com.baasbox.filters.LoggingFilter.class};
	}


	  

	
}