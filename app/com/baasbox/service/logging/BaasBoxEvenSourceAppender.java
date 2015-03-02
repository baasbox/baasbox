package com.baasbox.service.logging;

import org.apache.commons.lang.time.FastDateFormat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.baasbox.BBInternalConstants;
import com.baasbox.service.events.EventsService;
import com.baasbox.service.events.EventsService.StatType;

public class BaasBoxEvenSourceAppender<E> extends  UnsynchronizedAppenderBase<E>{

	private final FastDateFormat sdf = FastDateFormat.getInstance(BBInternalConstants.DATE_FORMAT_STRING);
		
	public static final String name = "BaasBoxAppender";
	public static final BaasBoxEvenSourceAppender<ILoggingEvent> appender = new BaasBoxEvenSourceAppender<ILoggingEvent>();
	
	protected BaasBoxEvenSourceAppender(){
		super();
		this.setName(name);
		this.start();
	}

	@Override
	protected void append(E message) {
		//TODO: this must be done in background to avoid blocking and/or performance issues
		String toSend=formattedMessage((ILoggingEvent)message);
		toSend=toSend.replace("\n", "<br>");
		//System.out.println("****BAASBOX_APPENDER***: " + toSend);
		EventsService.publish(StatType.SYSTEM_LOGGER, toSend);
	}
	
	protected String formattedMessage(ILoggingEvent message){
	    StringBuilder sb = new StringBuilder();
	    sb.append("\n").append(sdf.format(message.getTimeStamp())).append(" - ");
	    sb.append('[').append(message.getLevel()).append("] ");
	    sb.append(" - ").append(message.getFormattedMessage());
	    if (message.getThrowableProxy() != null)
	    	sb.append("\n").append(ThrowableProxyUtil.asString(message.getThrowableProxy()));
	    return sb.toString();
	}


		public String addClassetoLevel(ILoggingEvent event) {
			switch (event.getLevel().levelInt){
				case Level.TRACE_INT:
					return "<span class=\"log trace\">[trace]</span>";
				case Level.DEBUG_INT:
					return "<span class=\"log debug\">[debug]</span>";
				case Level.INFO_INT:
					return "<span class=\"log info\">[info ]</span>";
				case Level.WARN_INT:
					return "<span class=\"log warn\">[warn ]</span>";
				case Level.ERROR_INT:
					return "<span class=\"log error\">[error]</span>";
				default:
					return "<span class=\"log default\">["+event.getLevel().levelStr+"]</span>";
			}
		}
		
	
}
