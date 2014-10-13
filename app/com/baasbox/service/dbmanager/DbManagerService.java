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

package com.baasbox.service.dbmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.xmlbeans.impl.piccolo.io.FileFormatException;

import play.Logger;
import play.libs.Akka;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import scala.concurrent.duration.Duration;

import com.baasbox.BBConfiguration;
import com.baasbox.BBInternalConstants;
import com.baasbox.configuration.IosCertificateHandler;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.db.DbHelper;
import com.baasbox.db.async.ExportJob;
import com.baasbox.util.FileSystemPathUtil;

public class DbManagerService {
	public static final String backupDir = BBConfiguration.getDBBackupDir();
	public static final String fileSeparator = System.getProperty("file.separator")!=null?System.getProperty("file.separator"):"/";

	/**
	 * This method generate a full dump of the db in an asyncronus task.
	 * 
	 * The async nature of the method DOES NOT ensure the creation of the file
	 * so, querying for the file name with the /admin/db/:filename could return a 404
	 * @param appcode
	 * @return
	 * @throws FileNotFoundException
	 */
	public static String exportDb(String appcode) throws FileNotFoundException{
		java.io.File dir = new java.io.File(backupDir);
		if(!dir.exists()){
			boolean createdDir = dir.mkdirs();
			if(!createdDir){
				throw new FileNotFoundException("unable to create backup dir");
			}
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String fileName = String.format("%s-%s.zip", sdf.format(new Date()),FileSystemPathUtil.escapeName(appcode));
		//Async task
		Akka.system().scheduler().scheduleOnce(
				Duration.create(2, TimeUnit.SECONDS),
				new ExportJob(dir.getAbsolutePath()+fileSeparator+fileName,appcode),
				Akka.system().dispatcher()
				); 
		return fileName;
	}
	
	public static void deleteExport(String fileName) throws FileNotFoundException, IOException{
		java.io.File file = new java.io.File(backupDir+fileSeparator+fileName);
		if(!file.exists()){
			throw new FileNotFoundException("Export " + fileName + " not found");
		}else{
			boolean deleted = false;
			try{
				FileUtils.forceDelete(file);
				deleted =true;
			}catch(IOException e){
				deleted = file.delete();
				if(deleted==false){
					file.deleteOnExit();
				}
			}
			if(!deleted){
				throw new IOException("Unable to delete export.It will be deleted on the next reboot."+fileName);
			}
		}
	}
	
	public static List<String> getExports(){
		java.io.File dir = new java.io.File(backupDir);
		if(!dir.exists()){
			dir.mkdirs();
		}
		Collection<java.io.File> files = FileUtils.listFiles(dir, new String[]{"zip"},false);
		File[] fileArr = files.toArray(new File[files.size()]);

		Arrays.sort(fileArr,LastModifiedFileComparator.LASTMODIFIED_REVERSE);

		List<String> fileNames = new ArrayList<String>();
		for (java.io.File file : fileArr) {
			fileNames.add(file.getName());
		}
		return fileNames;
	}
	
	
	
	public static void importDb(String appcode,ZipInputStream zis) throws FileFormatException,Exception{
		String fileContent = null;
			try{
				//get the zipped file list entry
				ZipEntry ze = zis.getNextEntry();
				if (ze==null) throw new FileFormatException("Looks like the uploaded file is not a valid export.");
				if(ze.isDirectory()){
					ze = zis.getNextEntry();
				}
				if(ze!=null){
					File newFile = File.createTempFile("export",".json");
					FileOutputStream fout = new FileOutputStream(newFile);
					for (int c = zis.read(); c != -1; c = zis.read()) {
						fout.write(c);
					}
					fout.close();
					fileContent = FileUtils.readFileToString(newFile);
					newFile.delete();
				}else{
					throw new FileFormatException("Looks like the uploaded file is not a valid export.");
				}
				ZipEntry manifest = zis.getNextEntry();
				if(manifest!=null){
					File manifestFile = File.createTempFile("manifest",".txt");
					FileOutputStream fout = new FileOutputStream(manifestFile);
					for (int c = zis.read(); c != -1; c = zis.read()) {
						fout.write(c);
					}
					fout.close();
					String manifestContent  = FileUtils.readFileToString(manifestFile);
					manifestFile.delete();
					Pattern p = Pattern.compile(BBInternalConstants.IMPORT_MANIFEST_VERSION_PATTERN);
					Matcher m = p.matcher(manifestContent);
					if(m.matches()){
						String version = m.group(1);
						if (version.compareToIgnoreCase("0.6.0")<0){ //we support imports from version 0.6.0
							throw new FileFormatException(String.format("Current baasbox version(%s) is not compatible with import file version(%s)",BBConfiguration.getApiVersion(),version));
						}else{
							if (Logger.isDebugEnabled()) Logger.debug("Version : "+version+" is valid");
						}
					}else{
						throw new FileFormatException("The manifest file does not contain a version number");
					}
				}else{
					throw new FileFormatException("Looks like zip file does not contain a manifest file");
				}
				if (Logger.isDebugEnabled()) Logger.debug("Importing: "+fileContent);
				if(fileContent!=null && StringUtils.isNotEmpty(fileContent.trim())){
					DbHelper.importData(appcode, fileContent);
					zis.closeEntry();
					zis.close();
				}else{
					throw new FileFormatException("The import file is empty");
				}
			}catch(FileFormatException e){
				Logger.error(e.getMessage());
				throw e;
			}catch(Throwable e){
				Logger.error(ExceptionUtils.getStackTrace(e));
				throw new Exception("There was an error handling your zip import file.", e);
			}finally{
				try {
					if(zis!=null){
						zis.close();
					}
				} catch (IOException e) {
					// Nothing to do here
				}
			}
	}//importDb

}
