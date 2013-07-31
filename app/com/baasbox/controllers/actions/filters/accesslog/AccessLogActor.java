package com.baasbox.controllers.actions.filters.accesslog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.UntypedActor;

public class AccessLogActor extends UntypedActor {

	  
	  @Override
	  public void onReceive(Object msg) {
		if (msg != null && msg instanceof String) {
	    AccessLogUtils.initLoggingIfNeeded();
	      Logger logger = LoggerFactory.getLogger(AccessLogUtils.ALOG_LOGGER_NAME);
	      String m = msg.toString();
	      logger.info(m);
	    } else {
	      unhandled(msg);
	    }
	  }
}