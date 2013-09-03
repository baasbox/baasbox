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
