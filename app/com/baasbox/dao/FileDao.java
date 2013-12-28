package com.baasbox.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class FileDao extends NodeDao  {
	public final static String MODEL_NAME="_BB_File";
	public final static String BINARY_FIELD_NAME = "file";
	public final static String CONTENT_TYPE_FIELD_NAME="contentType";
	public final static String CONTENT_LENGTH_FIELD_NAME="contentLength";
	public static final String FILENAME_FIELD_NAME="fileName";
	private static final String RESIZED_IMAGE_FIELD_NAME="resized";
	
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
		file.field(CONTENT_LENGTH_FIELD_NAME,new Long(content.length));
		return file;
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}

	public ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
		QueryParams criteria=QueryParams.getInstance().where("id=?").params(new String[]{id});
		List<ODocument> listOfFiles = this.get(criteria);
		if (listOfFiles==null || listOfFiles.size()==0) return null;
		ODocument doc=listOfFiles.get(0);
		try{
			checkModelDocument((ODocument)doc);
		}catch(InvalidModelException e){
			//the id may reference a ORecordBytes which is not a ODocument
			throw new InvalidModelException("the id " + id + " is not a file " + this.MODEL_NAME);
		}
		return doc;
	}	
	
	public  byte[] getStoredResizedPicture(ODocument file, String sizePattern) throws InvalidModelException{
		super.checkModelDocument(file);
		Map<String,ORID> resizedMap=(Map<String,ORID>) file.field(RESIZED_IMAGE_FIELD_NAME);
		if (resizedMap!=null && resizedMap.containsKey(sizePattern)){
			ORecordBytes obytes = (ORecordBytes) resizedMap.get(sizePattern);
			return obytes.toStream();
		}
		return null;
	}
	
	public  void storeResizedPicture(ODocument file,String sizePattern, byte[] resizedImage) throws InvalidModelException {
		super.checkModelDocument(file);
		Map<String,ORID> resizedMap=(Map<String,ORID>) file.field(RESIZED_IMAGE_FIELD_NAME);
		if (resizedMap==null) resizedMap=new HashMap<String,ORID>();
		resizedMap.put(sizePattern, new ORecordBytes().fromStream(resizedImage).save().getIdentity());
		file.field(RESIZED_IMAGE_FIELD_NAME,resizedMap);
		this.save(file);
	}

}
