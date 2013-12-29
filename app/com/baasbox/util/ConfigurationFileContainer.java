package com.baasbox.util;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class ConfigurationFileContainer {
	public final static String BINARY_FIELD_NAME = "file";
	
	@JsonProperty
	private String name;
	@JsonProperty
	private byte[] content;
	
	@JsonCreator
	public ConfigurationFileContainer(@JsonProperty("name") String name,
									  @JsonProperty("content")byte[] content){
		this.name = name;
		this.content =content;
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

	
	
	
	
	
	
	
}
