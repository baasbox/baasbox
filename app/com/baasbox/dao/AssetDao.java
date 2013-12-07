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
package com.baasbox.dao;

import java.util.List;

import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.enumerations.Permissions;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class AssetDao extends NodeDao {
	protected final static String MODEL_NAME="_BB_asset";

	protected AssetDao(String modelName) {
		super(modelName);
	}
	
	public static AssetDao getInstance(){
		return new AssetDao(MODEL_NAME);
	}
	
	@Override
	@Deprecated
	public ODocument create() throws Throwable{
		throw new IllegalAccessError("Use create(String name) instead");
	}
	
	public ODocument create(String name) throws Throwable{
		ODocument asset=super.create();
		asset.field("name",name);
		super.grantPermission(asset, Permissions.ALLOW_READ,DefaultRoles.getORoles());
		return asset;
	}
	
	@Override
	public  void save(ODocument document) throws InvalidModelException{
		super.save(document);
	}
	
	public ODocument getByName (String name) throws SqlInjectionException{
		QueryParams criteria=QueryParams.getInstance().where("name=?").params(new String[]{name});
		List<ODocument> listOfAssets = this.get(criteria);
		if (listOfAssets==null || listOfAssets.size()==0) return null;
		return listOfAssets.get(0);
	}
}
