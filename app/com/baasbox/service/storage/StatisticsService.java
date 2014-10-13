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
package com.baasbox.service.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import play.Logger;

import com.baasbox.BBConfiguration;
import com.baasbox.dao.AssetDao;
import com.baasbox.dao.CollectionDao;
import com.baasbox.dao.DocumentDao;
import com.baasbox.dao.FileDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @author Claudio Tesoriero
 *
 */
public class StatisticsService {

		public static ImmutableMap data() throws SqlInjectionException, InvalidCollectionException{
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			UserDao userDao = UserDao.getInstance();
			CollectionDao collDao = CollectionDao.getInstance();
			AssetDao assetDao = AssetDao.getInstance();
			FileDao fileDao = FileDao.getInstance();
			ODatabaseRecordTx db = DbHelper.getConnection();
			
			long usersCount =userDao.getCount();
			long assetsCount = assetDao.getCount();
			long collectionsCount = collDao.getCount();
			long filesCount = fileDao.getCount();
			
			List<ODocument> collections = collDao.get(QueryParams.getInstance());
			ArrayList<ImmutableMap> collMap = collectionsDetails(collections);
			ImmutableMap response = ImmutableMap.of(
					"users", usersCount,
					"collections", collectionsCount,
					"collections_details", collMap,
					"assets",assetsCount,
					"files",filesCount
					);
			if (Logger.isDebugEnabled()) Logger.debug(response.toString());
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return response;
		}

		/**
		 * @param collections list of ODocuments representing the defined collections
		 * @return for each collection: its name, the number of documents, and the total size that the collection takes up
		 * @throws InvalidCollectionException
		 */
		public static ArrayList<ImmutableMap> collectionsDetails(List<ODocument> collections)	throws InvalidCollectionException {
			ODatabaseRecordTx db = DbHelper.getConnection();
			ArrayList<ImmutableMap> collMap = new ArrayList<ImmutableMap>();
			for(ODocument doc:collections){
				String collectionName = doc.field(CollectionDao.NAME);
				DocumentDao docDao = DocumentDao.getInstance(collectionName);
				long numberOfRecords=0;
				try{
					numberOfRecords=docDao.getCount();
					OClass myClass = db.getMetadata().getSchema().getClass(collectionName);
					long size=0;
					for (int clusterId : myClass.getClusterIds()) {
					  size += db.getClusterRecordSizeById(clusterId);
					}
					collMap.add(ImmutableMap.of(
							"name",collectionName,
							"records", numberOfRecords,
							"size",size
							));
				}catch (Throwable e){
					Logger.error(ExceptionUtils.getFullStackTrace(e));
				}
			}
			return collMap;
		}
		
		public static String dbConfiguration() {
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			OGlobalConfiguration.dumpConfiguration(ps);
			String content = "";
			try {
				content=baos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				content=baos.toString();
			}
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return content;
		}
		
		public static ImmutableMap db() {
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			ODatabaseRecordTx db = DbHelper.getConnection();
			HashMap dbProp= new HashMap();
			dbProp.put("version", OConstants.getVersion());
			dbProp.put("url", OConstants.ORIENT_URL);
			if (BBConfiguration.getStatisticsSystemOS()) dbProp.put("path", db.getStorage().getConfiguration().getDirectory());
			else dbProp.put("path", "N/A");
			dbProp.put("timezone", db.getStorage().getConfiguration().getTimeZone());
			dbProp.put("locale.language", db.getStorage().getConfiguration().getLocaleLanguage());
			dbProp.put("locale.country", db.getStorage().getConfiguration().getLocaleCountry());
			
			HashMap map = new HashMap();
			map.put("properties", dbProp);
			map.put("status", db.getStatus());
			map.put("configuration", dbConfiguration());
			map.put("physical_size", DbHelper.getDBTotalSize());
			map.put("datafile_freespace", DbHelper.getDBStorageFreeSpace());
			map.put("size_threshold_percentage", BBConfiguration.getDBAlertThreshold());
			
			
			ImmutableMap response=ImmutableMap.builder().build().copyOf(map);

			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return response;
		}
		
		public static ImmutableMap os() {
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			ImmutableMap response=null;
			if (BBConfiguration.getStatisticsSystemOS()){
				response = ImmutableMap.of(
						"os_name", System.getProperty("os.name"),
						"os_arch",  System.getProperty("os.arch"),
						"os_version",  System.getProperty("os.version"),
						"processors",  Runtime.getRuntime().availableProcessors()
						);
			}else{
				response = ImmutableMap.of(
						"os_name", "N/A",
						"os_arch", "N/A",
						"os_version",  "N/A",
						"processors",  0
						);				
			}
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return response;
		}
		
		public static ImmutableMap java() {
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			ImmutableMap response = ImmutableMap.of(
					"java_class_version", System.getProperty("java.class.version"),
					"java_vendor",  System.getProperty("java.vendor"),
					"java_vendor_url",  System.getProperty("java.vendor.url"),
					"java_version",  System.getProperty("java.version")
					);
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return response;
		}	
		
		public static ImmutableMap memory() {
			if (Logger.isTraceEnabled()) Logger.trace("Method Start");
			ImmutableMap response=null;

			if (BBConfiguration.getStatisticsSystemMemory()){
				Runtime rt = Runtime.getRuntime(); 
				long maxMemory=rt.maxMemory();
				long freeMemory=rt.freeMemory();
				long totalMemory=rt.totalMemory();
				response = ImmutableMap.of(
						"max_allocable_memory",maxMemory,
						"current_allocate_memory", totalMemory,
						"used_memory_in_the_allocate_memory",totalMemory - freeMemory,
						"free_memory_in_the_allocated_memory", freeMemory
						);
			}else{
				response = ImmutableMap.of(
						"max_allocable_memory",0,
						"current_allocate_memory", 0,
						"used_memory_in_the_allocate_memory",0 ,
						"free_memory_in_the_allocated_memory", 0
						);
			}
			
			if (Logger.isTraceEnabled()) Logger.trace("Method End");
			return response;
		}	
		
}
