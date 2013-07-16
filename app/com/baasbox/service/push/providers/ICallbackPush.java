package com.baasbox.service.push.providers;

public interface ICallbackPush {
	
	public boolean onSuccess();
	public boolean onError(String error);
}
