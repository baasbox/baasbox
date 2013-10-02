package com.baasbox.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;
import play.Play;

import com.baasbox.BBConfiguration;
import com.baasbox.util.ConfigurationFileContainer;

public class IosCertificateHandler implements IPropertyChangeCallback{


	static String sep = System.getProperty("file.separator")!=null?System.getProperty("file.separator"):"/";
	static String folder = BBConfiguration.getPushCertificateFolder();
	@Override
	public void change(Object iCurrentValue, Object iNewValue) {

		if(iNewValue==null){
			return;
		}
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
		if(iCurrentValue!=null){
			if(iCurrentValue instanceof String){
				try {
					currentValue =new ObjectMapper().readValue(iCurrentValue.toString(), ConfigurationFileContainer.class);
				} catch (Exception e) {
					Logger.debug("unable to convert value to ConfigurationFileContainer");
				}
			}else if (iCurrentValue instanceof ConfigurationFileContainer){
				currentValue = (ConfigurationFileContainer)iCurrentValue;
				}
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
				ByteArrayInputStream bais = new ByteArrayInputStream(newValue.getContent());
				try{
					FileUtils.copyInputStreamToFile(bais, newFile);
					bais.close();
				}catch(IOException ioe){
					//TODO:more specific exception
					throw new RuntimeException(ioe.getMessage());
				}

			}else{
				Logger.warn("Ios Certificate Handler invoked with wrong parameters");
				//TODO:throw an exception?
			}



		}

		public static void init(){
			String folder = BBConfiguration.getPushCertificateFolder();
			File f = new File(Play.application().path().getAbsolutePath()+sep+folder);
			if(!f.exists()){
				f.mkdir();
			}
			ConfigurationFileContainer prod = Push.PRODUCTION_IOS_CERTIFICATE.getValueAsFileContainer();
			ConfigurationFileContainer sandbox = Push.SANDBOX_IOS_CERTIFICATE.getValueAsFileContainer();
			if(prod!=null){
				Logger.debug("Creating production certificate:"+prod.getName());
				File prodCertificate =  new File(Play.application().path().getAbsolutePath()+sep+folder+sep+prod.getName());
				if(!prodCertificate.exists()){
					try{
						prodCertificate.createNewFile();
						ByteArrayInputStream bais = new ByteArrayInputStream(prod.getContent());
						FileUtils.copyInputStreamToFile(bais, prodCertificate);

					}catch(Exception e){
						prodCertificate.delete();
						throw new RuntimeException("Unable to create file for certificate:"+e.getMessage());
					}
				}
			}
			if(sandbox!=null){
				Logger.debug("Creating sandbox certificate:"+sandbox.getName());
				File sandboxCertificate =  new File(Play.application().path().getAbsolutePath()+sep+folder+sep+sandbox.getName());
				if(!sandboxCertificate.exists()){
					try{
						sandboxCertificate.createNewFile();
						ByteArrayInputStream bais = new ByteArrayInputStream(sandbox.getContent());
						FileUtils.copyInputStreamToFile(bais, sandboxCertificate);

					}catch(Exception e){
						sandboxCertificate.delete();
						throw new RuntimeException("Unable to create file for certificate:"+e.getMessage());
					}
				}
			}

		}

		public static File getCertificate(String name) {
			return new File(Play.application().path().getAbsolutePath()+sep+BBConfiguration.getPushCertificateFolder()+sep+name);
		}


	}
