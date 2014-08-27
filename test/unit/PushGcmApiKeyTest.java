package unit;
import org.junit.Assert;

import com.baasbox.controllers.Admin;
import com.baasbox.service.push.providers.GCMServer;

public class PushGcmApiKeyTest {

		public void PushGcmApiKey(){
			try {
				//Admin.setConfiguration(Push, subSection, key, value)
				GCMServer gcmserver = new GCMServer();
			} catch(Throwable e) {
				Assert.fail("API_KEY not valid:" +e.getMessage());
			}
		}
		
}
