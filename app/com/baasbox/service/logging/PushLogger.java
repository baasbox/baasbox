package com.baasbox.service.logging;

import java.util.ArrayList;

import com.google.common.base.Joiner;


public class PushLogger {

	private static ThreadLocal<PushLogger> logger = new ThreadLocal<PushLogger>() {
		protected PushLogger initialValue() {return new PushLogger();};
	};
	
	private ArrayList<String> logs=new ArrayList<String>();
	private boolean logEnabled=false;
	
	public static PushLogger getInstance(){
		return logger.get();
	}
	
	public PushLogger init(){
		logs.clear();
		return this;
	}
	
	public PushLogger enable(){
		logEnabled=true;
		return this;
	}
	
	public PushLogger disable(){
		logEnabled=false;
		return this;
	}
	
	public boolean isEnabled(){
		return logEnabled;
	}
	
	public PushLogger addMessage(String message){
		if (logEnabled) logs.add(message);
		return this;
	}

	public PushLogger addMessage(String message, Object... params){
		if (logEnabled) logs.add(String.format(message, params));
		return this;
	}

	public String[] messages(){
		return logs.toArray(new String[]{});
	}
	
	@Override
	public String toString(){
		return Joiner.on(System.lineSeparator()).join(logs);
	}
	
	private PushLogger() {}


}
