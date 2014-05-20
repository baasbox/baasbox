/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.db.async;

import play.Logger;

import com.baasbox.db.DbHelper;

public class ImportJob implements Runnable{

	private String content;
	private String appcode;
	byte[] buffer = new byte[2048];
	public ImportJob(String appcode,String content){
		this.content = content;
		this.appcode = appcode;
	}

	@Override
	public void run() {
		try{    
			DbHelper.importData(appcode,content);
		}catch(Exception e){
			Logger.error(e.getMessage());
			throw new RuntimeException(e);
		}

	}



}
