package unit;

import org.junit.Assert;
import org.junit.Test;

import com.notnoop.apns.APNS;
import com.notnoop.apns.PayloadBuilder;

public class PushCreatePayloadBuilderTest {
	
	//Test for issue #357
	@Test
	public void PushCreatePayloadBuilder(){
		try{	
			APNS apns = null;
			PayloadBuilder pb = apns.newPayload();
		}
		catch(Throwable e){
			Assert.fail("Test failed:" +e.getMessage());
		}
	}
}
