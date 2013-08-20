package com.baasbox.filters {

	import play.api.mvc._
	import play.api.libs.concurrent.Execution.Implicits.defaultContext	
	import Results._
	import org.slf4j._
	import play.api.Logger
	import java.util.Date
	

	class LoggingFilter extends Filter {
  		val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
  		def apply(next: (RequestHeader) => Result)(rh: RequestHeader) = {
    		val start = System.currentTimeMillis

    		def logTime(result: PlainResult): Result = {
      			val time = System.currentTimeMillis - start
      			val dateFormatted = new Date(start)
      			val userAgent = rh.headers.get("User-Agent").getOrElse("")
      			filterLogger.info(s"${rh.remoteAddress}\t[${dateFormatted}]\t${rh.method}\t${rh.uri}\t${rh.version}\t${result.header.status}\t${userAgent}\t${rh.contentType}\t-\t${time}")
      			result
    		}
    
   			 next(rh) match {
      			case plain: PlainResult => logTime(plain)
      			case async: AsyncResult => async.transform(logTime)
    		}
    	}
    }
}
  
	
