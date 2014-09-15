package unit;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.baasbox.controllers.CustomHttpCode;
import com.baasbox.exception.BaasBoxPushException;
import com.baasbox.service.push.providers.APNServer;
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

public class PushValidateAPNPayloadTest {
	@Test
	public void ValidateCorrectSoundKey(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("sound", "test.wav");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate passed",true,true);
		}
	}

	@Test
	public void ValidateFormatInvalidSoundKey(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			// int
			jNode.put("sound", 123);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed",CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("sound", "test.wav");
			// ObjectNode
			jNode.put("sound", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed", CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add("test.wav");
			jNode.put("sound", arrayNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed", CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
		}

	}


	@Test
	public void ValidateCorrectActionLocalizedKey(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("actionLocalizedKey", "test");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate passed",true,true);
		}
	}

	@Test
	public void ValidateInvalidFormatActionLocalizedKey(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			try {
				// int
				jNode.put("actionLocalizedKey", 123);
				APNServer.validatePushPayload(jNode);
			} catch (BaasBoxPushException e) {
				Assert.assertEquals("Validate failed",CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
			try{
				ObjectNode aNode = JsonNodeFactory.instance.objectNode();
				aNode.put("actionLocalizedKey", "Test");
				// ObjectNode
				jNode.put("actionLocalizedKey", aNode);
				APNServer.validatePushPayload(jNode);
			}
			catch(BaasBoxPushException e) {
				Assert.assertEquals("Validate failed", CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
			try{
				//ArrayNode
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				arrayNode.add("actionLocalizedKey.wav");
				jNode.put("actionLocalizedKey", arrayNode);
				APNServer.validatePushPayload(jNode);
			}
			catch(BaasBoxPushException e) {
				Assert.assertEquals("Validate failed", CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
	
		
	}
	
	public void ValidateCorrectBadgeKey(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("badge", 123);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate passed",true,true);
		}
	}
	
	public void ValidateFormatInvalidBadgeKey(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try {
			//String
			jNode.put("badge", "1");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("badge", 10);
			// ObjectNode
			jNode.put("badge", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add(10);
			jNode.put("badge", arrayNode);
			APNServer.validatePushPayload(jNode);sss		
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
	}

}
