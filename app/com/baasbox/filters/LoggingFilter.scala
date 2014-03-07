package com.baasbox.filters {
  
	import play.api.Logger
	import org.slf4j.LoggerFactory
	import play.api.mvc._
	import scala.concurrent.Future
	import play.api.libs.concurrent.Execution.Implicits.defaultContext
	import com.baasbox.BBConfiguration
	import java.util.Date
		
	class LoggingFilter extends Filter {
	  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
	           (requestHeader: RequestHeader): Future[SimpleResult] = {
	    val start = System.currentTimeMillis
	    nextFilter(requestHeader).map { result =>
	      if(BBConfiguration.getWriteAccessLog()){
			val time = System.currentTimeMillis - start
			val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
			val dateFormatted = new Date(start)
			val userAgent = requestHeader.headers.get("User-Agent").getOrElse("")
			val contentLength = result.header.headers.get("Content-Length").getOrElse("-")
			/*
			* Log format is the combined one: http://httpd.apache.org/docs/2.2/logs.html
			* Unfortunely we have to do a litlle hack to log the authenticated username due a limitation of the framework: scala cannot access to the current Http Context where the username is stored
			*/
			val username = result.header.headers.get("BB-USERNAME").getOrElse("-")
			result.withHeaders("BB-USERNAME"->"")
			result.withHeaders("X-BB-Request-Time" -> time.toString)
			filterLogger.info(s"""${requestHeader.remoteAddress}\t-\t${username}\t[${dateFormatted}]\t${"\""}${requestHeader.method} ${requestHeader.uri} ${requestHeader.version}${"\""}\t${result.header.status}\t${contentLength}\t${"\""}${"\""}\t${"\""}${userAgent}${"\""}\t${time}""")
	      }
		  result
	    }
	  }
	}
  
}
  
	
