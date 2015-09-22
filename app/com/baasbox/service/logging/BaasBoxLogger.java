/*
     Copyright 
     BAASBOX  - info-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.baasbox.service.logging;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

import play.Logger.ALogger;

/**
 * Wrapper for LogBack/Play logging system
 *
 */
public class BaasBoxLogger  {
	
	private static String EVENT_SOURCE_LOGGER="application.baasbox.eventsource";

	public static final ALogger eventSourceLogger = play.Logger.of(EVENT_SOURCE_LOGGER);
	public static final org.slf4j.Logger playUnderlyingLogger = play.Logger.underlying();
	
	//this is not thread safe, but ATM this is not a problem and we do not want to create a bottleneck 
	private static boolean isEventSourceLoggerEnabled=false;
	
	public static void startEventSourceLogging(){
		isEventSourceLoggerEnabled=true;
		ch.qos.logback.classic.Logger logger = ((ch.qos.logback.classic.Logger)
				LoggerFactory.getLogger(EVENT_SOURCE_LOGGER)) ;
        if (logger.getAppender(BaasBoxEvenSourceAppender.name) == null) 
        	logger.addAppender(BaasBoxEvenSourceAppender.appender);
	}
	
	public static void stopEventSourceLogging(){
		isEventSourceLoggerEnabled=false;
		 ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger(EVENT_SOURCE_LOGGER)) 
         	.detachAppender(BaasBoxEvenSourceAppender.name);
	}
	
	public static boolean isEventSourceLoggingEnabled(){
		return isEventSourceLoggerEnabled;
	}
	
	public static boolean isTraceEnabled() {
        return play.Logger.isTraceEnabled() || (isEventSourceLoggerEnabled && eventSourceLogger.isTraceEnabled());
    }

    public static boolean isDebugEnabled() {
        return play.Logger.isDebugEnabled() || (isEventSourceLoggerEnabled && eventSourceLogger.isDebugEnabled());
    }

    public static boolean isInfoEnabled() {
        return play.Logger.isInfoEnabled() || (isEventSourceLoggerEnabled && eventSourceLogger.isInfoEnabled());
    }

    public static boolean isWarnEnabled() {
        return play.Logger.isWarnEnabled() || (isEventSourceLoggerEnabled && eventSourceLogger.isWarnEnabled());
    }

    public static boolean isErrorEnabled() {
        return play.Logger.isErrorEnabled() || (isEventSourceLoggerEnabled && eventSourceLogger.isErrorEnabled());
    }
    
    public static void trace(String message) {
        play.Logger.trace(message);
        eventSourceLogger.trace(message);
    }
    
    public static void trace(String message, Object... args) {
        play.Logger.trace(message, args);
        if (isEventSourceLoggerEnabled) eventSourceLogger.trace(message,args);
    }

    public static void trace(String message, Throwable error) {
        play.Logger.trace(message, error);
        if (isEventSourceLoggerEnabled) eventSourceLogger.trace(message,error);
    }

    public static void debug(String message) {
    	message=StringUtils.abbreviateMiddle(message, "....... <cut: this message exceeds 2048 chars> .......", 2103); //2048 + the message
    	play.Logger.debug(message);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.debug(message);
    }

    public static void debug(String message, Object... args) {
    	play.Logger.debug(message, args);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.debug(message,args);
    }


    public static void debug(String message, Throwable error) {
    	play.Logger.debug(message, error);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.debug(message,error);
    }

    public static void info(String message) {
    	play.Logger.info(message);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.info(message);
    }


    public static void info(String message, Object... args) {
    	play.Logger.info(message, args);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.info(message,args);
    }


    public static void info(String message, Throwable error) {
    	play.Logger.info(message, error);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.info(message, error);
    }


    public static void warn(String message) {
    	play.Logger.warn(message);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.warn(message);
    }


    public static void warn(String message, Object... args) {
    	play.Logger.warn(message, args);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.warn(message,args);
    }

 
    public static void warn(String message, Throwable error) {
    	play.Logger.warn(message, error);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.warn(message,error);
    }

 
    public static void error(String message) {
    	play.Logger.error(message);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.error(message);
    }


    public static void error(String message, Object... args) {
    	play.Logger.error(message, args);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.error(message,args);
    }

 
    public static void error(String message, Throwable error) {
    	play.Logger.error(message, error);
    	if (isEventSourceLoggerEnabled) eventSourceLogger.error(message, error);
    }

}
