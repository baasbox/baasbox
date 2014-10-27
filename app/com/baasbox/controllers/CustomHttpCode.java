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

package com.baasbox.controllers;

import play.mvc.Results.Status;
import play.mvc.Results;

public enum CustomHttpCode {
			PUSH_SENT_WITH_ERROR(20001,200,"Push notifications were sent but they may be subject to loss of data. HINT: check push settings in console","ok, with a reservation"),
			DOCUMENT_VERSION(40001,400,"You are attempting to update a database object with older data. Versions is not the same","error"),
			ACL_JSON_FIELD_MALFORMED(40002,400,"The 'acl' field is not a valid JSON string","error"),
			ACL_PERMISSION_UNKNOWN(40003,400,"The specified 'permission' is unknown. Valid ones are 'read','update','delete',all'","error"),
			ACL_USER_OR_ROLE_KEY_UNKNOWN(40004,400,"Only 'users' and 'roles' can be used","error"),
			ACL_USER_DOES_NOT_EXIST(40005,400,"The specified user does not exist","error"),
			ACL_ROLE_DOES_NOT_EXIST(40006,400,"The specified role does not exist","error"),
			JSON_VALUE_MUST_BE_ARRAY(40010,400,"The expected JSON value must be an array '[.., .., ..]'","error"),
			JSON_PAYLOAD_NULL(40011,400,"The body payload cannot be empty.","error"),
			PUSH_MESSAGE_INVALID(40020,400,"The body payload doesn't contain the 'message' key or message is NOT a String","error"),
			PUSH_PROFILE_FORMAT_INVALID(40021,400,"Push profile invalid. It must be an Array of integer and accepted values are 1,2 or 3","error"),			
			PUSH_USERS_FORMAT_INVALID(40022,400,"Users MUST be an array of String","error"),
			PUSH_NOTFOUND_KEY_USERS(40023,400,"The body payload doesn't contain key users","error"),
			PUSH_SOUND_FORMAT_INVALID(40024,400,"Sound value MUST be a String","error"),
			PUSH_BADGE_FORMAT_INVALID(40025,400,"Badge value MUST be a number","error"),
			PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID(40026,400,"ActionLocalizedKey MUST be a String","error"),
			PUSH_LOCALIZED_KEY_FORMAT_INVALID(40027,400,"LocalizedKey MUST be a String","error"),
			PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID(40028,400,"LocalizedArguments MUST be an Array of String","error"),
			PUSH_COLLAPSE_KEY_FORMAT_INVALID(40029,400,"Collapse_key MUST be a String","error"),
			PUSH_TIME_TO_LIVE_FORMAT_INVALID(40030,400,"Time_to_live MUST be a positive number or equal zero","error"),
			PUSH_CONTENT_AVAILABLE_FORMAT_INVALID(40031,400,"Content-available MUST be an Integer (1 for silent notification)","error"),
			PUSH_CATEGORY_FORMAT_INVALID(40032,400,"Category MUST be a String","error"),
			SESSION_TOKEN_EXPIRED (40101,401,"Authentication info not valid or not provided. HINT: is your session expired?","error"),
			PUSH_CONFIG_INVALID (50301,503,"Push settings are not properly configured. HINT: go to administration console and check the settings","error"),
			PUSH_HOST_UNREACHABLE(50302,503,"The server cannot resolve the host name. HINT: check your internet connection.","error"),
			PUSH_INVALID_REQUEST(50303,503,"Could not send push notifications. HINT: Check your API Key(Google), it's possible that push service aren't enabled in Google Play Developer Console","error"),
			PUSH_INVALID_APIKEY(50304,503,"Could not save API KEY. HINT: Check your API Key, it's possible that push service aren't enabled in the Google Play Developer Console","error"),
			PUSH_PROFILE_DISABLED(50305,503,"Push app disabled, one or more app are disabled","error"),
			PUSH_SWITCH_EXCEPTION(50306,503,"Cannot switch, because settings for the selected mode are missing","error"),

			;
			private String type;
			private int bbCode;
			private int httpCode;
			private String description;

			private CustomHttpCode(int bbCode, int httpCode, String description, String type){
				this.bbCode=bbCode;
				this.httpCode=httpCode;
				this.description=description;
				this.type=type;
			}
			
			public String getType() {
				return type;
			}
			
			public int getBbCode(){
				return this.bbCode;
			}
			
			public int getHttpCode(){
				return this.httpCode;
			}
			
			public String getDescription(){
				return this.description;
			}
			
			public Status getStatus(){
				return Results.status(this.bbCode, this.description);
			}
			
			public static CustomHttpCode getFromBbCode(int bBcode){
				for (CustomHttpCode v: values()){
					if (v.getBbCode()==bBcode) return v;
				}
				return null;
			}
			
}
