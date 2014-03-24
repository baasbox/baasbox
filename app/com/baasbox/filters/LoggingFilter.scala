package com.baasbox.filters {

	import play.api.mvc._
	import play.api.libs.concurrent.Execution.Implicits.defaultContext	
	import Results._
	import org.slf4j._
	import play.api.Logger
	import java.util.Date
	import java.text.SimpleDateFormat
	import com.baasbox.BBConfiguration
	import com.baasbox.metrics.BaasBoxMetric
	import com.codahale.metrics.Timer
	

	class LoggingFilter extends Filter {

	  		def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
	  		    val start = System.currentTimeMillis()
				var timers = BaasBoxMetric.Track.startRequest(rh.method,rh.uri)

	    		def logTime(result: PlainResult): Result = {
	    				
	    				var contentLength = ""
	  				try{
		  			    val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
		      			val time = System.currentTimeMillis() - start
		      			val dateFormatted = new Date(start)
		      			val dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")

		      			val userAgent = rh.headers.get("User-Agent").getOrElse("")
		      			contentLength = result.header.headers.get("Content-Length").getOrElse("-")
		      			if(BBConfiguration.getWriteAccessLog()){
			      			/*
			      			* Log format is the combined one: http://httpd.apache.org/docs/2.2/logs.html
			      			* Unfortunely we have to do a litlle hack to log the authenticated username due a limitation of the framework: scala cannot access to the current Http Context where the username is stored
			      			*/
			      			val username = result.header.headers.get("BB-USERNAME").getOrElse("-")
			      			result.withHeaders("BB-USERNAME"->"")
			      			//remote-address username [request-datetime] "HTTP-method URI HTTP-version" HTTP_STATUS_CODE content-length  "user agent" process-time
			      			filterLogger.info(s"""${rh.remoteAddress} \t${username} \t[${dateFormat.format(dateFormatted)}] \t${"\""}${rh.method} ${rh.uri} ${rh.version}${"\""} \t${result.header.status} \t${contentLength} \t${"\""}${userAgent}${"\""} \t${time}ms""")
			  			}
			  		}finally{
			  			BaasBoxMetric.Track.endRequest(timers,result.header.status,contentLength)
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
  
	
