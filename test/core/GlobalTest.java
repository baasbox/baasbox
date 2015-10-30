package core;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import play.Configuration;

import com.baasbox.Global;
import com.typesafe.config.Config;

public class GlobalTest extends Global{
	private Config config;
	private Configuration fullConfiguration;
	
	private static AtomicInteger count = new AtomicInteger(0);

	@Override
	public Configuration onLoadConfig(Configuration extConfiguration, File path,
			ClassLoader classloader) {
		
		Configuration configuration = new Configuration(this.config);
		
		
		Configuration finalConf = new Configuration(extConfiguration.getWrappedConfiguration().$plus$plus(configuration.getWrappedConfiguration()));
		
		System.out.println("GlobalTest - onLoadConfig ("+count.getAndIncrement()+"): " + configuration.asMap().toString());
		System.out.println("GlobalTest - onLoadConfig B ("+count.get()+"): " + finalConf.asMap().toString());
		
		
		return super.onLoadConfig(finalConf, path, classloader);
	}

	public void setTestConfiguration(Config additionalConfig) {
		this.config=additionalConfig;
	}
	public Config getTestConfiguration() {
		return this.config;
	}
	
}
