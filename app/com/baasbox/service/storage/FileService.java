package com.baasbox.service.storage;

import java.util.List;

import com.baasbox.dao.FileDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class FileService {
	public static final String DATA_FIELD_NAME="attachedData";
	public static final String BINARY_FIELD_NAME=FileDao.BINARY_FIELD_NAME;
	public final static String CONTENT_TYPE_FIELD_NAME=FileDao.CONTENT_TYPE_FIELD_NAME;
	public final static String CONTENT_LENGTH_FIELD_NAME=FileDao.CONTENT_LENGTH_FIELD_NAME;
	
		public static ODocument createFile(String fileName,String data,String contentType, byte[] content) throws Throwable{
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,content);
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}

		
		public static ODocument getById(String id) throws SqlInjectionException {
			FileDao dao = FileDao.getInstance();
			return dao.getById(id);
		}
		
		public static void deleteById(String id) throws Throwable, SqlInjectionException, FileNotFoundException{
			FileDao dao = FileDao.getInstance();
			ODocument file=getById(id);
			if (file==null) throw new FileNotFoundException();
			dao.delete(file.getIdentity());
		}


		public static List<ODocument> getFiles(QueryParams criteria) throws SqlInjectionException {
			FileDao dao = FileDao.getInstance();
			return dao.get(criteria);
		}	
}
