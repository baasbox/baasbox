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

package com.baasbox.service.sociallogin;

import com.fasterxml.jackson.databind.JsonNode;



public class BaasBoxFacebookException extends BaasBoxSocialException {

	public BaasBoxFacebookException() {
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(JsonNode jsonNode) {
		super(jsonNode);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(String arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public BaasBoxFacebookException(Throwable arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getMessage() {
		return (error.get("message")==null?"":error.get("message").asText());
	}

	@Override
	public String getErrorType() {
		return (error.get("type")==null?"":error.get("type").asText());
	}

	@Override
	public String getErrorCode() {
		return (error.get("code")==null?"":error.get("code").asText());
	}

	@Override
	public String getErrorSubCode() {
		return (error.get("error_subcode")==null?"":error.get("error_subcode").asText());
	}

}
