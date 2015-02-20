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
import play.Logger;
import play.Logger.ALogger;

/**
 * Wrapper for LogBack/Play logging system
 *
 */
public class BoxLogger  {
	
	private static String EVENT_SOURCE_LOGGER="application.baasbox.eventsource";

	public static final ALogger eventSourceLogger = play.Logger.of(EVENT_SOURCE_LOGGER);
	public static final org.slf4j.Logger playUnderlyingLogger = play.Logger.underlying();
	
	public static boolean isTraceEnabled() {
        return Logger.isTraceEnabled() || eventSourceLogger.isTraceEnabled();
    }

    public static boolean isDebugEnabled() {
        return Logger.isDebugEnabled() || eventSourceLogger.isDebugEnabled();
    }

    public static boolean isInfoEnabled() {
        return Logger.isInfoEnabled() || eventSourceLogger.isInfoEnabled();
    }

    public static boolean isWarnEnabled() {
        return Logger.isWarnEnabled() || eventSourceLogger.isWarnEnabled();
    }

    public static boolean isErrorEnabled() {
        return Logger.isErrorEnabled() || eventSourceLogger.isErrorEnabled();
    }
    
    public static void trace(String message) {
        Logger.trace(message);
        eventSourceLogger.trace(message);
    }
    
    public static void trace(String message, Object... args) {
        Logger.trace(message, args);
        eventSourceLogger.trace(message,args);
    }

    public static void trace(String message, Throwable error) {
        Logger.trace(message, error);
        eventSourceLogger.trace(message,error);
    }

    public static void debug(String message) {
    	Logger.debug(message);
    	eventSourceLogger.debug(message);
    }

    public static void debug(String message, Object... args) {
    	Logger.debug(message, args);
    	eventSourceLogger.debug(message,args);
    }


    public static void debug(String message, Throwable error) {
    	Logger.debug(message, error);
    	eventSourceLogger.debug(message,error);
    }

    public static void info(String message) {
    	Logger.info(message);
    	eventSourceLogger.info(message);
    }


    public static void info(String message, Object... args) {
    	Logger.info(message, args);
    	eventSourceLogger.info(message,args);
    }


    public static void info(String message, Throwable error) {
    	Logger.info(message, error);
    	eventSourceLogger.info(message, error);
    }


    public static void warn(String message) {
    	Logger.warn(message);
    	eventSourceLogger.warn(message);
    }


    public static void warn(String message, Object... args) {
    	Logger.warn(message, args);
    	eventSourceLogger.warn(message,args);
    }

 
    public static void warn(String message, Throwable error) {
    	Logger.warn(message, error);
    	eventSourceLogger.warn(message,error);
    }

 
    public static void error(String message) {
    	Logger.error(message);
    	eventSourceLogger.error(message);
    }


    public static void error(String message, Object... args) {
    	Logger.error(message, args);
    	eventSourceLogger.error(message,args);
    }

 
    public static void error(String message, Throwable error) {
    	Logger.error(message, error);
    	eventSourceLogger.error(message, error);
    }

}
