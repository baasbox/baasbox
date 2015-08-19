package com.baasbox.controllers.helpers;

import scala.concurrent._
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.iteratee._
import akka.util.Timeout

/***
 * see https://stackoverflow.com/questions/28461877/is-there-a-bug-in-play2-testing-with-fakerequests-and-chunked-responses-enumera
 * 
 */
object BaasBoxHelpers {
def contentAsBytes(of: Future[SimpleResult]): Array[Byte] = {
    val result = Await.result(of, Duration.Inf)
    val eBytes = result.header.headers.get(HttpConstants.Headers.TRANSFER_ENCODING) match {
      case Some("chunked") => result.body &> Results.dechunk
      case _ => result.body
    }
    Await.result(eBytes |>>> Iteratee.consume[Array[Byte]](), Duration.Inf)
  }
}
