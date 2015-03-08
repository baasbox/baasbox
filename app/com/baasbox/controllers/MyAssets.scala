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


import play.api.mvc._;
import play.api.libs.concurrent.Execution.Implicits._;

object MyAssets extends Controller {
	def at(path: String, file: String) = EssentialAction { request =>
		controllers.Assets.at(path, file)(request).map { result =>
			if (result.header.status == NOT_FOUND) {
				NotFound("Resource " + path + file + ", method GET was not found on this server.")
			} else result
		}
	}
}