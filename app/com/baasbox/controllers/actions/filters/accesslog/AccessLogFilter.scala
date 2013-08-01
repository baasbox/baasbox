package com.baasbox.filters {

	import play.api.mvc._
	import play.api.libs.concurrent.Execution.Implicits.defaultContext	
	import Results._
	import org.slf4j._
	import play.api.Logger
	import java.util.Date
	

	class LoggingFilter extends Filter {
  		val filterLogger = LoggerFactory.getLogger("com.baasbox.filters")
  		def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
    		val start = System.currentTimeMillis

    		def logTime(result: PlainResult): Result = {
      			val time = System.currentTimeMillis - start
      			val dateFormatted = new Date(start)
      			val userAgent = rh.headers.apply("User-Agent")
      			filterLogger.info(s"${rh.remoteAddress} [${dateFormatted}] ${rh.method} ${rh.uri} ${rh.version} ${result.header.status} ${userAgent} - ${time} msec - ${rh.contentType}")
      			result
    		}
    
   			 next(rh) match {
      			case plain: PlainResult => logTime(plain)
      			case async: AsyncResult => async.transform(logTime)
    		}
    	}
    }
}
  
	
