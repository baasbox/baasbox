package com.baasbox.db.async;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import play.Logger;

import com.baasbox.BBConfiguration;
import com.baasbox.BBInternalConstants;
import com.baasbox.db.DbHelper;

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
		try{
			File f = new File(this.fileName);
			dest = new FileOutputStream(f);
			zip = new ZipOutputStream(dest);
			
			ByteArrayOutputStream content =new ByteArrayOutputStream(); 
			DbHelper.exportData(this.appcode,content);
			
			byte[] contentArr = content.toByteArray();
			Logger.info(String.format("Writing %d bytes ",contentArr.length));
			File tmpJson = File.createTempFile("export", ".json");
			File manifest = File.createTempFile("manifest", ".txt");
			
			
			
			FileUtils.writeByteArrayToFile(tmpJson, contentArr, false);
			FileUtils.writeStringToFile(manifest, BBInternalConstants.IMPORT_MANIFEST_VERSION_PREFIX+BBConfiguration.getApiVersion());
			
			ZipEntry entry = new ZipEntry("export.json");
			zip.putNextEntry(entry);
    		zip.write(FileUtils.readFileToByteArray(tmpJson));
    		zip.closeEntry();
    		
    		ZipEntry entryManifest = new ZipEntry("manifest.txt");
			zip.putNextEntry(entryManifest);
    		zip.write(FileUtils.readFileToByteArray(manifest));
    		zip.closeEntry();
    		
    		tmpJson.delete();
    		manifest.delete();
    		content.close();
    		

		}catch(Exception e){
			Logger.error(e.getMessage());
		}finally{
			try{
				if(zip!=null)
					zip.close();
				if(dest!=null)
					dest.close();
			
			}catch(Exception ioe){
				ioe.getStackTrace();
				Logger.error(ioe.getMessage());
			}
		}
	}
	
	
	

}
