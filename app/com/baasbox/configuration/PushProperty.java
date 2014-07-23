package com.baasbox.configuration;

public class PushProperty {

	private String key;
	private String description;
	private Class<?> type;
	
	public PushProperty(String iprofileName, String ikey, String idescription,
			Class<?> itype) {
		key=ikey;
		description=idescription;
		type=itype;
	}
	
}
