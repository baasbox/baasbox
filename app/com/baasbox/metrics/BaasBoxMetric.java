package com.baasbox.metrics;

import static com.codahale.metrics.MetricRegistry.name;

import org.apache.commons.lang3.StringUtils;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public class BaasBoxMetric {
	
	public static final String TIMER_UPTIME = "server.uptime.timer";
	public static final String TIMER_REQUESTS="requests.timer";
	public static final String METER_REQUESTS="requests.meter";
	public static final String HISTOGRAM_RESPONSE_SIZE="responses.size";
	public static final String GAUGE_MEMORY_MAX_ALLOCABLE="memory.max_allocable";
	private static final String GAUGE_MEMORY_CURRENT_ALLOCATE = "memory.current_allocate";
	private static final String GAUGE_MEMORY_USED = "memory.used";
	
	
	public final static MetricRegistry 	registry = new MetricRegistry();

	static {
		//memory gauges
		registry.register(name(GAUGE_MEMORY_MAX_ALLOCABLE),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
                		long maxMemory=rt.maxMemory();
                		return maxMemory;
                    }
                });
		
		registry.register(name(GAUGE_MEMORY_CURRENT_ALLOCATE),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
        				long totalMemory=rt.totalMemory();
        				return totalMemory;        				
                    }
                });
		
		registry.register(name(GAUGE_MEMORY_USED),
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                    	Runtime rt = Runtime.getRuntime(); 
        				long freeMemory=rt.freeMemory();
        				long totalMemory=rt.totalMemory();
        				return totalMemory - freeMemory;
        			}
                });	
	}

	public static class Track {
		
		private static Timer.Context uptime;
		
		public static void startUptime(){
				uptime=registry.timer(name(TIMER_UPTIME)).time();
		}
		
		public static void stopUptime(){
			uptime.stop();
		}
		
		public static Timer.Context[] startRequest(String method,String uri){
			registry.meter(name(METER_REQUESTS)).mark();
			Timer.Context timer1=  registry.timer(name(TIMER_REQUESTS)).time();
			Timer.Context timer2=  registry.timer(name(TIMER_REQUESTS,method,uri)).time();
			return new Timer.Context[] {timer1,timer2};
		}
		
		
		public static void endRequest(Timer.Context[] timers, int status,String responseSize){
			if (!StringUtils.isEmpty(responseSize) && ! responseSize.equals("-"))
				registry.histogram(name(HISTOGRAM_RESPONSE_SIZE)).update(Long.parseLong(responseSize));
			for (Timer.Context timer: timers) timer.stop();
		}		
		
	}
}
