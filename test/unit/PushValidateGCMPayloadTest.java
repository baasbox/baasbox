package unit;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.service.push.providers.APNServer;
import com.baasbox.service.push.providers.GCMServer;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.notnoop.apns.APNS;
import com.notnoop.apns.PayloadBuilder;

public class PushValidateGCMPayloadTest {
	@Test
	public void ValidateCorrectCollapseKey(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("collapse_key", "It's a String");
			GCMServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for collapse_key",true,true);
		}
	}

	@Test
	public void ValidateFormatInvalidCollapseKey(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			// int
			jNode.put("collapse_key", 123);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed for collapse_key",CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("collapse_key", "It's an ObjectNode");
			// ObjectNode
			jNode.put("collapse_key", aNode);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for collapse_key", CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add("It's an element of Array for collapse_key");
			jNode.put("collapse_key", arrayNode);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for collapse_key", CustomHttpCode.PUSH_COLLAPSE_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}

	}

	@Test
	public void ValidateCorrectTimeToLive(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("time_to_live", 100);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for time_to_live",true,true);
		}
	}

	@Test
	public void ValidateFormatInvalidTimeToLive(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			// negative int
			jNode.put("time_to_live", -123);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed for time_to_live",CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			// String
			jNode.put("time_to_live", "It's a String for time_to_live");
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for time_to_live", CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("time_to_live", "It's an ObjectNode for time_to_live");
			// ObjectNode
			jNode.put("time_to_live", aNode);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for time_to_live", CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add("It's an element of Array for time_to_live");
			jNode.put("time_to_live", arrayNode);
			GCMServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for time_to_live", CustomHttpCode.PUSH_TIME_TO_LIVE_FORMAT_INVALID.getDescription(),e.getMessage());
		}

	}





}
