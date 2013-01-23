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