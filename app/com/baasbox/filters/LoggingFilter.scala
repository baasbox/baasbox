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

package com.baasbox.filters {
  
	import play.api.Logger
	import org.slf4j.LoggerFactory
	import play.api.mvc._
	import scala.concurrent.Future
	import play.api.libs.concurrent.Execution.Implicits.defaultContext
	import com.baasbox.BBConfiguration
	import java.util.Date
	import java.text.SimpleDateFormat
	import com.baasbox.metrics.BaasBoxMetric
	import com.codahale.metrics.Timer

		
	class LoggingFilter extends Filter {
	  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
	           (requestHeader: RequestHeader): Future[SimpleResult] = {


	    val start = System.currentTimeMillis
	    var timers = BaasBoxMetric.Track.startRequest(requestHeader.method,requestHeader.uri)
	    var contentLength = ""
	    
	    nextFilter(requestHeader).map { result =>
	    	try{
				val time = System.currentTimeMillis - start
				val filterLogger = LoggerFactory.getLogger("com.baasbox.accesslog")
				val dateFormatted = new Date(start)
				val dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
				val userAgent = requestHeader.headers.get("User-Agent").getOrElse("")
				contentLength = result.header.headers.get("Content-Length").getOrElse("-")
				if(BBConfiguration.getWriteAccessLog()){
					/*
					* Log format is the combined one: http://httpd.apache.org/docs/2.2/logs.html
					* Unfortunely we have to do a litlle hack to log the authenticated username due a limitation of the framework: scala cannot access to the current Http Context where the username is stored
					*/
					val username = result.header.headers.get("BB-USERNAME").getOrElse("-")
					result.withHeaders("BB-USERNAME"->"")
					result.withHeaders("X-BB-Request-Time" -> time.toString)
					filterLogger.info(s"""${requestHeader.remoteAddress} \t${username} \t[${dateFormat.format(dateFormatted)}] \t${"\""}${requestHeader.method} ${requestHeader.uri} ${requestHeader.version}${"\""} \t${result.header.status} \t${contentLength} \t${"\""}${userAgent}${"\""} \t${time}ms""")
		        }
		    }finally{
	  			BaasBoxMetric.Track.endRequest(timers,result.header.status,contentLength)
			}
		    result

	    }
	  }
	}
  
}
  
	
