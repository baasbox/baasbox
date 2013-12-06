package com.baasbox.service.storage;

import com.baasbox.dao.FileDao;
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

}
