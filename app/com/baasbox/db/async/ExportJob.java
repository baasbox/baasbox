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

package com.baasbox.db.async;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.baasbox.BBConfiguration;
import com.baasbox.BBInternalConstants;
import com.baasbox.db.DbHelper;
import com.baasbox.service.logging.BaasBoxLogger;

public class ExportJob implements Runnable{

	
	private String fileName;
	private String appcode;
	public ExportJob(String fileName,String appcode){
		this.fileName = fileName;
		this.appcode = appcode;
	}
	
	@Override
	public void run() {
		FileOutputStream dest = null;
		ZipOutputStream zip = null;
		FileOutputStream tempJsonOS = null;
		FileInputStream in = null;
		try{
			File f = new File(this.fileName);
			dest = new FileOutputStream(f);
			zip = new ZipOutputStream(dest);
			
			File tmpJson = File.createTempFile("export",".json");
			tempJsonOS = new FileOutputStream(tmpJson);
			DbHelper.exportData(this.appcode,tempJsonOS);
			BaasBoxLogger.info(String.format("Writing %d bytes ",tmpJson.length()));
			tempJsonOS.close();
			
			ZipEntry entry = new ZipEntry("export.json");
			zip.putNextEntry(entry);
				in = new FileInputStream(tmpJson);
				final int BUFFER = BBConfiguration.getExportBufferSize(); 
		        byte buffer[] = new byte[BUFFER];
		        
				int length;
		        while ((length = in.read(buffer)) > 0) {
		            zip.write(buffer, 0, length);
		        }
    		zip.closeEntry();
    		in.close();
    		
    		File manifest = File.createTempFile("manifest", ".txt");
			FileUtils.writeStringToFile(manifest, BBInternalConstants.IMPORT_MANIFEST_VERSION_PREFIX+BBConfiguration.getApiVersion());
    		
    		ZipEntry entryManifest = new ZipEntry("manifest.txt");
				zip.putNextEntry(entryManifest);
				zip.write(FileUtils.readFileToByteArray(manifest));
    		zip.closeEntry();
    		
    		tmpJson.delete();
    		manifest.delete();

		}catch(Exception e){
			BaasBoxLogger.error(ExceptionUtils.getMessage(e));
		}finally{
			try{
				if(zip!=null)
					zip.close();
				if(dest!=null)
					dest.close();
				if(tempJsonOS!=null)
					tempJsonOS.close();
				if(in!=null)
					in.close();
			
			}catch(Exception ioe){
				BaasBoxLogger.error(ExceptionUtils.getMessage(ioe));
			}
		}
	}
	
	
	

}
