package com.baasbox.filters {

	import play.api.mvc._
	import play.api.libs.concurrent.Execution.Implicits.defaultContext	
	import Results._
	import org.slf4j._
	import play.api.Logger
	import java.util.Date
	import com.baasbox.BBConfiguration
	

	class LoggingFilter extends Filter {

	  		def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
	  		    val start = System.currentTimeMillis
	
	    		def logTime(result: PlainResult): Result = {
	  			  if(BBConfiguration.getWriteAccessLog()){
	  			    val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
	      			val time = System.currentTimeMillis - start
	      			val dateFormatted = new Date(start)
	      			val userAgent = rh.headers.get("User-Agent").getOrElse("")
	      			val contentLength = result.header.headers.get("Content-Length").getOrElse("-")
	      			/*
	      			* Log format is the combined one: http://httpd.apache.org/docs/2.2/logs.html
	      			* Unfortunely we have to do a litlle hack to log the authenticated username due a limitation of the framework: scala cannot access to the current Http Context where the username is stored
	      			*/
	      			val username = result.header.headers.get("BB-USERNAME").getOrElse("-")
	      			result.withHeaders("BB-USERNAME"->"")
	      			filterLogger.info(s"""${rh.remoteAddress}\t-\t${username}\t[${dateFormatted}]\t${"\""}${rh.method} ${rh.uri} ${rh.version}${"\""}\t${result.header.status}\t${contentLength}\t${"\""}${"\""}\t${"\""}${userAgent}${"\""}\t${time}""")
	  			  }
	      		  result
	    		}
	    
	   			 next(rh) match {
	      			case plain: PlainResult => logTime(plain)
	      			case async: AsyncResult => async.transform(logTime)
	    		}
	    	
	    }
    }
}
  
	
