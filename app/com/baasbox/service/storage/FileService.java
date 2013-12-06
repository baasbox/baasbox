package com.baasbox.service.storage;

import com.baasbox.dao.AssetDao;
import com.baasbox.dao.FileDao;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.exception.AssetNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class FileService {
	private static final String DATA_FIELD_NAME="attachedData";

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
}
