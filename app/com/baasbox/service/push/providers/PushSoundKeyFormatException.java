package com.baasbox.service.push.providers;

import com.baasbox.exception.BaasBoxPushException;

public class PushSoundKeyFormatException extends BaasBoxPushException {
	public PushSoundKeyFormatException(String message){
		super(message);
	}
	
	public PushSoundKeyFormatException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public PushSoundKeyFormatException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushSoundKeyFormatException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
