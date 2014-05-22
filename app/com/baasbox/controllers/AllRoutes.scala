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

package com.baasbox.controllers


import play.api.mvc._
import play.api.Routes


object AllRoutes extends Controller{
	val routeCache = {
	    import routes._
	    val jsRoutesClass = classOf[routes.javascript]
	    val controllers = jsRoutesClass.getFields().map(_.get(null))
	    controllers.flatMap { controller =>
	        controller.getClass().getDeclaredMethods().map { action =>
	            action.invoke(controller).asInstanceOf[play.core.Router.JavascriptReverseRoute]
	        }
	    }
	}
	
	def javascriptRoutes = Action { implicit request =>
	    Ok(Routes.javascriptRouter("BBRoutes")(routeCache:_*)).as("text/javascript")
	}
}