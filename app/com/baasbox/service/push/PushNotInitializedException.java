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

package com.baasbox.service.push;

public class PushNotInitializedException extends PushConfigurationException {
	
	public PushNotInitializedException(String message){
		super(message);
	}
	
	public PushNotInitializedException() {
		super();
		// TODO Auto-generated constructor stub
	}



	public PushNotInitializedException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public PushNotInitializedException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}
}
