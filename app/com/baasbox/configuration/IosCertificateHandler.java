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

package com.baasbox.configuration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		File f = new File(folder);
		if(!f.exists()){
			f.mkdirs();
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
					if (Logger.isDebugEnabled()) Logger.debug("unable to convert value to ConfigurationFileContainer");
				}
			}else if (iCurrentValue instanceof ConfigurationFileContainer){
				currentValue = (ConfigurationFileContainer)iCurrentValue;
				}
			}
			if(currentValue!=null){
				File oldFile =  new File(folder+sep+currentValue.getName());
				if(oldFile.exists()){
					try{
						FileUtils.forceDelete(oldFile);
					}catch(Exception e){
						Logger.error(e.getMessage());
					}
				}
			}
			if(newValue!=null){
				File newFile =  new File(folder+sep+newValue.getName());
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
			File f = new File(folder);
			if(!f.exists()){
				f.mkdirs();
			}
			ConfigurationFileContainer prod = Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE.getValueAsFileContainer();
			ConfigurationFileContainer sandbox = Push.PROFILE1_SANDBOX_IOS_CERTIFICATE.getValueAsFileContainer();
			
			ConfigurationFileContainer prod2 = Push.PROFILE2_PRODUCTION_IOS_CERTIFICATE.getValueAsFileContainer();
			ConfigurationFileContainer sandbox2 = Push.PROFILE2_SANDBOX_IOS_CERTIFICATE.getValueAsFileContainer();
			
			ConfigurationFileContainer prod3 = Push.PROFILE3_PRODUCTION_IOS_CERTIFICATE.getValueAsFileContainer();
			ConfigurationFileContainer sandbox3 = Push.PROFILE3_SANDBOX_IOS_CERTIFICATE.getValueAsFileContainer();
			
			if(prod!=null){
				if (Logger.isDebugEnabled()) Logger.debug("Creating production certificate for default profile:"+prod.getName());
				File prodCertificate =  new File(folder+sep+prod.getName());
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
				if (Logger.isDebugEnabled()) Logger.debug("Creating sandbox certificate for default profile:"+sandbox.getName());
				File sandboxCertificate =  new File(folder+sep+sandbox.getName());
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
			
			if(prod2!=null){
				if (Logger.isDebugEnabled()) Logger.debug("Creating production certificate for profile 2:"+prod2.getName());
				File prodCertificate =  new File(folder+sep+prod2.getName());
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
			if(sandbox2!=null){
				if (Logger.isDebugEnabled()) Logger.debug("Creating sandbox certificate for profile 2:"+sandbox2.getName());
				File sandboxCertificate =  new File(folder+sep+sandbox2.getName());
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
			
			if(prod3!=null){
				if (Logger.isDebugEnabled()) Logger.debug("Creating production certificate for profile 3:"+prod3.getName());
				File prodCertificate =  new File(folder+sep+prod3.getName());
				if(!prodCertificate.exists()){
					try{
						prodCertificate.createNewFile();
						ByteArrayInputStream bais = new ByteArrayInputStream(prod3.getContent());
						FileUtils.copyInputStreamToFile(bais, prodCertificate);

					}catch(Exception e){
						prodCertificate.delete();
						throw new RuntimeException("Unable to create file for certificate:"+e.getMessage());
					}
				}
			}
			if(sandbox3!=null){
				if (Logger.isDebugEnabled()) Logger.debug("Creating sandbox certificate for profile 3:"+sandbox3.getName());
				File sandboxCertificate =  new File(folder+sep+sandbox3.getName());
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
			return new File(BBConfiguration.getPushCertificateFolder()+sep+name);
		}


	}
