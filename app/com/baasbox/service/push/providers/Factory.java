/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
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

package com.baasbox.service.push.providers;


public class Factory {
	public enum VendorOS{
		IOS("ios"), ANDROID("android");
		
		String os;
		
		VendorOS(String os){
			this.os=os;
		}
		
		public String toString(){
			return os;
		}
		
		public static VendorOS getVendorOs(String os){
			for (VendorOS vos : values()){
				if (vos.toString().equalsIgnoreCase(os)) return vos;
			}
			return null;
		}
	}
	
	public enum ConfigurationKeys{
		ANDROID_API_KEY,APPLE_TIMEOUT,IOS_CERTIFICATE,IOS_CERTIFICATE_PASSWORD,IOS_SANDBOX
	}
	public static IPushServer getIstance(VendorOS vendor){
		switch (vendor) {
			case IOS:
				return new APNServer();
			case ANDROID:
				return new GCMServer();
		}
		return null;
	}
}




