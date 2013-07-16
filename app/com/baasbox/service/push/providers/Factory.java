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




