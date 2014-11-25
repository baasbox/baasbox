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
	public void ValidateCorrectContentAvailable(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("content-available", 1);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for content-available",true,true);
		}
	}
	
	@Test
	public void ValidateFormatInvalidContentAvailable(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try {
			//String
			jNode.put("content-available", "1");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for content-available",CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("content-available", 10);
			// ObjectNode
			jNode.put("content-available", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for content-available",CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add(10);
			jNode.put("content-available", arrayNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for content-available",CustomHttpCode.PUSH_CONTENT_AVAILABLE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
	}
	
	
	
	
	@Test
	public void ValidateCorrectCategory(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("category", "this is a category");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for category",true,true);
		}
	}
	
	
	@Test
	public void ValidateFormatInvalidCategory(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			// int
			jNode.put("category", 123);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed for category",CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("category", "test.wav");
			// ObjectNode
			jNode.put("category", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for category",CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add("category");
			jNode.put("category", arrayNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for category",CustomHttpCode.PUSH_CATEGORY_FORMAT_INVALID.getDescription(),e.getMessage());
		}

	}
	

	
	
	@Test
	public void ValidateCorrectSound(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("sound", "test.wav");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for sound",true,true);
		}
	}

	@Test
	public void ValidateFormatInvalidSound(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			// int
			jNode.put("sound", 123);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed for sound",CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("sound", "test.wav");
			// ObjectNode
			jNode.put("sound", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for sound", CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add("test.wav");
			jNode.put("sound", arrayNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for sound", CustomHttpCode.PUSH_SOUND_FORMAT_INVALID.getDescription(),e.getMessage());
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
			Assert.assertEquals("Test failed for ActionLocalizedKey",true,true);
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
				Assert.assertEquals("Validate failed for ActionLocalizedKey",CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
			try{
				ObjectNode aNode = JsonNodeFactory.instance.objectNode();
				aNode.put("actionLocalizedKey", "Test");
				// ObjectNode
				jNode.put("actionLocalizedKey", aNode);
				APNServer.validatePushPayload(jNode);
			}
			catch(BaasBoxPushException e) {
				Assert.assertEquals("Validate failed for ActionLocalizedKey", CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
			try{
				//ArrayNode
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				arrayNode.add("array action localized key");
				jNode.put("actionLocalizedKey", arrayNode);
				APNServer.validatePushPayload(jNode);
			}
			catch(BaasBoxPushException e) {
				Assert.assertEquals("Validate failed for ActionLocalizedKey", CustomHttpCode.PUSH_ACTION_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
			}
	
		
	}
	
	@Test
	public void ValidateCorrectBadge(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("badge", 123);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for Badge",true,true);
		}
	}
	
	@Test
	public void ValidateFormatInvalidBadge(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try {
			//String
			jNode.put("badge", "1");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Badge",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("badge", 10);
			// ObjectNode
			jNode.put("badge", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Badge",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add(10);
			jNode.put("badge", arrayNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Badge",CustomHttpCode.PUSH_BADGE_FORMAT_INVALID.getDescription(),e.getMessage());
		}
	}
	
	@Test
	public void ValidateCorrectLocalizedKey(){
		try{	
			ObjectNode jNode = JsonNodeFactory.instance.objectNode();
			jNode.put("localizedKey", "TEST");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Test failed for Localized Key",true,true);
		}
	}
	
	@Test
	public void ValidateFormatInvalidLocalizedKey(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try {
			// int
			jNode.put("localizedKey", 3);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Localized Key",CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try{
			ObjectNode aNode = JsonNodeFactory.instance.objectNode();
			aNode.put("localizedKey", 10);
			// ObjectNode
			jNode.put("localizedKey", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Localized Key",CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			//ArrayNode
			ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
			arrayNode.add(10);
			jNode.put("localizedKey", arrayNode);
			APNServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Localized Key",CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
	}
	
	@Test
	public void ValidateCorrectLocalizedArguments(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{
			ArrayNode aNode = JsonNodeFactory.instance.arrayNode();
			aNode.add("NODE-ARRAY-LOCALIZED-ARGUMENTS");
			// ArrayNode
			jNode.put("localizedArguments", aNode);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Test failed for Localized Arguments",true,true);
		}
	}
	
	@Test
	public void ValidateFormatInvalidLocalizedArguments(){
		ObjectNode jNode = JsonNodeFactory.instance.objectNode();
		try{	
			jNode.put("localizedKey", "LOCALIZED-ARGUMENTS");
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e){
			Assert.assertEquals("Validate failed for Localized Arguments",CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			// int
			jNode.put("localizedArguments", 30);
			APNServer.validatePushPayload(jNode);
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Localized Arguments",CustomHttpCode.PUSH_LOCALIZED_ARGUMENTS_FORMAT_INVALID.getDescription(),e.getMessage());
		}
		try {
			//ObjectNode
			ObjectNode arrayNode = JsonNodeFactory.instance.objectNode();
			arrayNode.put("localizedArguments","Object-Node-LOCALIZED-ARGUMENTS");
			jNode.put("localizedKey", arrayNode);
			APNServer.validatePushPayload(jNode);	
		}
		catch(BaasBoxPushException e) {
			Assert.assertEquals("Validate failed for Localized Arguments",CustomHttpCode.PUSH_LOCALIZED_KEY_FORMAT_INVALID.getDescription(),e.getMessage());
		}
	}
	
	

}
