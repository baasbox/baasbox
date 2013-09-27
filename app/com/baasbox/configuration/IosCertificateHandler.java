package com.baasbox.configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import play.Logger;
import play.Play;

import com.baasbox.BBConfiguration;
import com.baasbox.util.ConfigurationFileContainer;

public class IosCertificateHandler implements IPropertyChangeCallback{
	
	
	static String sep = System.getProperty("file.separator")!=null?System.getProperty("file.separator"):"/";
	
	@Override
	public void change(Object iCurrentValue, Object iNewValue) {
		Logger.warn("newValue: "+iNewValue+" oldValue:"+iCurrentValue );
		String folder = BBConfiguration.getPushCertificateFolder();
		File f = new File(Play.application().path().getAbsolutePath()+sep+folder);
		if(!f.exists()){
			f.mkdir();
		}
		ConfigurationFileContainer newValue=null;
		ConfigurationFileContainer currentValue=null;
		if(iNewValue!=null && iNewValue instanceof ConfigurationFileContainer){
			
			 newValue =(ConfigurationFileContainer)iNewValue;
		}
		if(iCurrentValue!=null && iCurrentValue instanceof ConfigurationFileContainer){
			
			currentValue =(ConfigurationFileContainer)iCurrentValue;
		}
		if(currentValue!=null){
			File oldFile =  new File(Play.application().path().getAbsolutePath()+sep+folder+sep+currentValue.getName());
			if(oldFile.exists()){
				try{
					FileUtils.forceDelete(oldFile);
				}catch(Exception e){
					Logger.error(e.getMessage());
				}
			}
		}
		if(newValue!=null){
			
			
			File newFile =  new File(Play.application().path().getAbsolutePath()+sep+folder+sep+newValue.getName());
			try{
			if(!newFile.exists()){
				newFile.createNewFile();
			}
			}catch(IOException ioe){
				throw new RuntimeException("unable to create file:"+ioe.getMessage());
			}
			Logger.debug("Is file "+ newValue.getName()+" null???");
			byte[] content = newValue.getContent();
			Logger.debug("Is file "+ newValue.getName()+" null???"+content.length);
			ByteArrayInputStream bais = new ByteArrayInputStream(newValue.getContent());
			try{
				FileUtils.copyInputStreamToFile(bais, newFile);
				bais.close();
			}catch(IOException ioe){
				//TODO:more specific exception
				throw new RuntimeException(ioe.getMessage());
			}
		
		}else{
			Logger.warn("Ios Certificate Handler invoked with wrong parameters:"+iNewValue.toString()+" and "+iCurrentValue.toString());
			//TODO:throw an exception?
		}
		
		
		
	}

	public File getCertificate(String name) {
		return new File(Play.application().path().getAbsolutePath()+sep+BBConfiguration.getPushCertificateFolder()+sep+name);
	}
	
	
}
