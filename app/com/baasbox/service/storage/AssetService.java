/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.service.storage;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.baasbox.dao.AssetDao;
import com.baasbox.dao.FileAssetDao;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.AssetNotFoundException;
import com.baasbox.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

public class AssetService {
	public static ODocument create(String name, String meta) throws Throwable{
		AssetDao dao = AssetDao.getInstance();
		ODocument doc=dao.create(name);
		try{
			
			if (meta!=null && !meta.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ 'meta' : " + meta + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
		}catch (Throwable e){
			throw e;
		}
		return doc;
	}
	
	public static ODocument createFile(String name,String fileName,String meta,String contentType, byte[] content) throws Throwable{
		FileAssetDao dao = FileAssetDao.getInstance();
		ODocument doc=dao.create(name,fileName,contentType,content);
		if (meta!=null && !meta.trim().isEmpty()) {
			ODocument metaDoc=(new ODocument()).fromJSON("{ 'meta' : " + meta + "}");
			doc.merge(metaDoc, true, false);
		}
		dao.save(doc);
		return doc;
	}
	
	public static ODocument get(String rid) throws SqlInjectionException, IllegalArgumentException, InvalidModelException {
		AssetDao dao = AssetDao.getInstance();
		return dao.get(rid);
	}
	
	public static ODocument getByName(String name) throws SqlInjectionException, IllegalArgumentException, InvalidModelException {
		AssetDao dao = AssetDao.getInstance();
		QueryParams criteria=QueryParams.getInstance().where("name=?").params(new String[]{name});
		List<ODocument> listOfAssets = getAssets(criteria);
		if (listOfAssets==null || listOfAssets.size()==0) return null;
		return listOfAssets.get(0);
	}
	
	public static ByteArrayOutputStream getFileAsStream (String fileAssetName) throws SqlInjectionException, IOException{
		FileAssetDao dao = FileAssetDao.getInstance();
		QueryParams criteria=QueryParams.getInstance().where("name=?").params(new String[]{fileAssetName});
		List<ODocument> listOfAssets = dao.get(criteria);
		if (listOfAssets==null || listOfAssets.size()==0) return null;
		ODocument fileAsset= listOfAssets.get(0);
		ORecordBytes record = fileAsset.field("file");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		record.toOutputStream(out);
		return out;
	}
	
	public static long getCount(QueryParams criteria) throws InvalidCollectionException, SqlInjectionException{
		AssetDao dao = AssetDao.getInstance();
		return dao.getCount(criteria);
	}
	
	public static List<ODocument> getAssets(QueryParams criteria) throws SqlInjectionException{
		AssetDao dao = AssetDao.getInstance();
		return dao.get(criteria);
	}
	
	public static void deleteByRid(String rid) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		dao.delete(rid);
	}
	public static void deleteByRid(ORID rid) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		dao.delete(rid);
	}	
	public static void deleteByName(String name) throws Throwable {
		AssetDao dao = AssetDao.getInstance();
		ODocument asset=getByName(name);
		if (asset==null) throw new AssetNotFoundException();
		dao.delete(asset.getIdentity());
	}	
}
