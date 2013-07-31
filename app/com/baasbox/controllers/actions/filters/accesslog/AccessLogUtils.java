package com.baasbox.controllers.actions.filters.accesslog;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;

/**
 * Actual impl that does the logging from all 3 code paths:
 *   java GlobalSettings, scala GlobalSettings, and Netty Custom Channel Handler
 * 
 * Also implements a JMX MXBean to control logging format
 * 
 * @author bguan
 *
 */
public class AccessLogUtils implements AccessLogMXBean {

  // This long format serves as demonstration of what is possible
  public static final String LONG_LOG_FMT = 
    "%RemoteAddr$s - %@Authorization$s - [%Timestamp$tF:%Timestamp$tT%Timestamp$tz] \"%Method$s %URI$s %ProtoVer$s\" %Status$d - %@User-Agent$s - %Duration$d msec - %!Content-Type$s - %ResponseSize$d bytes\n" +
    "\nRequest Headers:\n%AllRequestHeaders$s\n" +
    "\nResponse Headers:\n%AllResponseHeaders$s\n";

  public static final String DEFAULT_LOG_FMT = 
    "%RemoteAddr$s - [%Timestamp$tc] \"%Method$s %URI$s %ProtoVer$s\" %Status$d - %@User-Agent$s - %Duration$d msec - %!Content-Type$s - %ResponseSize$d bytes";

  public static final String ALOG_LOGGER_NAME = "alog.logger";
  public static final String ALOG_APPENDER_NAME = "alog.appender";
  
  // eager singleton
  private static AccessLogUtils _self = new AccessLogUtils();
  
  private String _log_fmt = null;
  private String _pos_based_fmt = null; // java Formatter syntax with 1 based position index
  private String[] _fields = null;
  
  private final ActorRef _alogActor;
  
  private AccessLogUtils() {

    setLoggingFormat(DEFAULT_LOG_FMT);
    
    ActorSystem system = ActorSystem.create("AlogAkkaSystem");
    _alogActor= system.actorOf(new Props(AccessLogActor.class), "alogActor");
    
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer(); 
    try {
      ObjectName name = new ObjectName("alog:type=AlogMXBean");
      try { mbs.unregisterMBean(name); } catch (Exception e) { /* do nothing */ }
      mbs.registerMBean(this, name);
    } catch (Exception e) {
      System.err.println("Alog initialization error");
      e.printStackTrace();
    }
    
  }
  
  // Check
  protected static void initLoggingIfNeeded() {
    // Check logback config if app has supplied specific Logger and Appender
    LoggerContext logctx = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger log = logctx.getLogger(ALOG_LOGGER_NAME);
    OutputStreamAppender<ILoggingEvent> appender = (OutputStreamAppender<ILoggingEvent>)log.getAppender(ALOG_APPENDER_NAME); 
    if (appender == null) {
      // if not, supplied default Console Appender using the simplest output of access log formatted output as is
      log.detachAndStopAllAppenders();
      appender = new ConsoleAppender<ILoggingEvent>();
      appender.setName(ALOG_APPENDER_NAME);
      appender.setContext(logctx);
      PatternLayout pl = new PatternLayout();
      pl.setPattern("%m%n)");
      pl.setContext(logctx);
      pl.start();
      appender.setLayout(pl);
      appender.start();
      log.setLevel(Level.DEBUG);
      log.setAdditive(false);
      log.addAppender(appender);
    }
  }
  
  
  public static AccessLogUtils getInstance() {
    return _self;
  }
  
  @Override
  public String getLoggingFormat() {
    return _log_fmt;
  }

  @Override
  public synchronized void setLoggingFormat(String format) {
    _log_fmt = format;
    
    // hand written tokenizer for efficiency and self sufficiency
    
    boolean inFieldToken = false; // activated by '$'
    boolean inFieldHeaderToken = false;  // activated by '@' after '$'
    int pos = 0;
    StringBuilder wipStrfmt = new StringBuilder();
    StringBuilder wipToken = new StringBuilder();
    List<String> wipFields = new ArrayList<String>(20);
    
    for(int c = 0; c < format.length(); c++) {
      char ch = format.charAt(c);
      if ( ch == '%') { 
        // begining of Field Name token
        inFieldToken = !inFieldToken;
        wipStrfmt.append(ch);
      } else if (inFieldToken) {
        if (ch == '$' || Character.isWhitespace(ch) || (!inFieldHeaderToken && ch != '@' && ch != '!' && !Character.isJavaIdentifierPart(ch))) {
          // end of Field Name token
          pos++;
          wipStrfmt.append(pos);
          wipStrfmt.append(ch);
          String field = wipToken.toString().trim();
          
          if (field.charAt(0)=='@' || field.charAt(0)=='!') {
            // change to Uppercase if Request/Response Header
            field = field.toUpperCase();
          } else {
           // force a check for invalid field if not a Request Header
            field = LogEntry.Field.valueOf(field).name(); 
          }
          wipFields.add(field);
          wipToken = new StringBuilder();
          inFieldToken = false;
          inFieldHeaderToken = false;
        } else if (ch == '@' || ch == '!') {
          inFieldHeaderToken = true;
          wipToken.append(ch);
        } else {
          // keep appending to Token
          wipToken.append(ch);
        }
      } else {
        // keep appending to String Format
        wipStrfmt.append(ch);
      }
    }
    
    _pos_based_fmt = wipStrfmt.toString();
    _fields = wipFields.toArray(new String[] {});
  }
  
  protected String format(LogEntry r) {
    Object[] args = r.pick(_fields);
    return String.format(_pos_based_fmt, args);
  }
  
  
  public void spool(LogEntry logEntry) {
    if (logEntry != null) {
      _alogActor.tell(format(logEntry),_alogActor);
    }
  }
}
