package com.baasbox.dao;

import com.baasbox.dao.exception.InvalidModelException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class FileDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_File";
	public final static String BINARY_FIELD_NAME = "file";
	public final static String CONTENT_TYPE_FIELD_NAME="contentType";
	public final static String CONTENT_LENGTH_FIELD_NAME="contentLength";
	
	public static final String DATA_FIELD_NAME="data";
	public static final String FILENAME_FIELD_NAME="fileName";
	
	protected FileDao(String modelName) {
		super(modelName);
	}
	
	public static FileDao getInstance(){
		return new FileDao(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name, String fileName, String contentType, byte[] content) instead");
	}
	
	public ODocument create(String fileName, String contentType, byte[] content) throws Throwable{
		ODocument file=super.create();
		ORecordBytes record = new ORecordBytes(content);
		file.field(BINARY_FIELD_NAME,record);
		file.field(FILENAME_FIELD_NAME,fileName);
		file.field(CONTENT_TYPE_FIELD_NAME,contentType);
		file.field(CONTENT_LENGTH_FIELD_NAME,content.length);
		return file;
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}	

}
