package com.baasbox.util;

import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class ConfigurationFileContainer {
	public final static String BINARY_FIELD_NAME = "file";
	
	
	private String name;
	private byte[] content;
	
	public ConfigurationFileContainer(String name,byte[] content){
		this.name = name;
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getContent() {
		return content;
	}
	
	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public String toString() {
		return this.name;
	}
	
	
	
	
	
	
	
}
