package unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Test;

import com.baasbox.service.query.JsonTree;
import com.baasbox.service.query.PartsLexer;
import com.baasbox.service.query.PartsParser;
public class JsonQueryTest {

	@Test
	public void readValueFromArray() throws Exception {

		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3]}";

		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".coll[0]", 1));

		PartsParser pp = new PartsParser(parts);

		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode root = JsonTree.read(json, pp);
		if(root.isMissingNode()){
			fail();
		}else{
			assertEquals(1,root.asInt());
		}
	}

	@Test
	public void readValueFromNonExistentSimpleField() throws Exception {

		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3]}";

		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".title2", 1));

		PartsParser pp = new PartsParser(parts);

		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode root = JsonTree.read(json, pp);
		if(root.isMissingNode()){
			assertEquals(1,1);
		}else{
			fail();
		}
	}
	
	@Test
	public void readValueFromSimpleField() throws Exception {

		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3]}";

		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".title", 1));

		PartsParser pp = new PartsParser(parts);

		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode root = JsonTree.read(json, pp);
		if(root.isMissingNode()){
			fail();
		}else{
			assertEquals("title", root.asText());
		}
	}


	@Test
	public void readValueFromNonExistentArrayMember() throws Exception {

		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3]}";

		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".coll[3]", 1));

		PartsParser pp = new PartsParser(parts);

		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode root = JsonTree.read(json, pp);
		if(root.isMissingNode()){
			assertEquals(1,1);
		}else{
			fail();
		}
	}
	
	@Test
	public void setValueToSimpleField() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"title2\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".title", 1));

		PartsParser pp = new PartsParser(parts);

		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		JsonTree.write(json, pp, jsonData.get("data"));
		assertEquals("title2",JsonTree.read(json, pp).asText());
		
		
	}
	
	@Test
	public void setValueToUnexistingSimpleField() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"title2\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".title2", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		JsonTree.write(json, pp, jsonData.get("data"));
		assertEquals("title2",JsonTree.read(json, pp).asText());
	}
	
	@Test
	public void setValueToNestedUnexistingSimpleField() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"t\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".title2", 1));
		parts.add(pl.parse(".firstLetter", 2));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			fail();
		}catch(Exception e){
			assertEquals("title2.firstLetter is not a valid path",e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayAdding() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"4\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".coll[3]", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals(4,JsonTree.read(json, pp).asInt(0));
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayReplacing$Int() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":10}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".coll[0]", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals(10,JsonTree.read(json, pp).asInt(0));
			
			JsonNode jn = json.get("coll");
			assertTrue(jn instanceof ArrayNode);
			ArrayNode an = JsonNodeFactory.instance.arrayNode();
			 an.add(10);
			 an.add(2);
			 an.add(3);
			ArrayNode result = (ArrayNode)jn;  
			assertEquals(result,an);
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayAdding$Int() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":4}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".coll[3]", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals(4,JsonTree.read(json, pp).asInt(0));
			
			JsonNode jn = json.get("coll");
			assertTrue(jn instanceof ArrayNode);
			ArrayNode an = JsonNodeFactory.instance.arrayNode();
			 an.add(1);
			 an.add(2);
			 an.add(3);
			 an.add(4);
			ArrayNode result = (ArrayNode)jn;  
			assertEquals(result,an);
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayReplacing$String() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"megatag\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".tags[0]", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals("megatag",JsonTree.read(json, pp).asText());
			
			JsonNode jn = json.get("tags");
			assertTrue(jn instanceof ArrayNode);
			ArrayNode an = JsonNodeFactory.instance.arrayNode();
			 an.add("megatag");
			 an.add("two");
			 an.add("three");
			ArrayNode result = (ArrayNode)jn;  
			assertEquals(result,an);
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayAdding$String() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":\"four\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".tags[3]", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals("four",JsonTree.read(json, pp).asText());
			
			JsonNode jn = json.get("tags");
			assertTrue(jn instanceof ArrayNode);
			ArrayNode an = JsonNodeFactory.instance.arrayNode();
			 an.add("one");
			 an.add("two");
			 an.add("three");
			 an.add("four");
			ArrayNode result = (ArrayNode)jn;  
			assertEquals(result,an);
		}catch(Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToRootAddingObject() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"]}";
		String data = "{\"data\":{\"name\":\"giastfader\"}}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".author", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			JsonNode n = JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals("giastfader",n.get("name").asText());
			assertTrue(json.has("author"));
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToRootReplacingObject() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"],\"author\":{\"name\":\"swampie\"}}";
		String data = "{\"data\":{\"name\":\"giastfader\"}}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".author", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			assertTrue(json.has("author"));
			assertEquals("swampie",json.get("author").get("name").asText());
			JsonNode n = JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals("giastfader",n.get("name").asText());
			assertTrue(json.has("author"));
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToNestedObject() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"],\"author\":{\"name\":\"swampie\"}}";
		String data = "{\"data\":{\"role\":\"Consultant\"}}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".author", 1));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			assertTrue(json.has("author"));
			assertFalse(json.get("author").has("role"));
			JsonNode n = JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals("Consultant",n.get("role").asText());
			assertTrue(json.get("author").has("role"));
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setValueToArrayInNestedObject() throws Exception{
		String content = "{\"@rid\":\"#20:0\",\"@version\":25,\"@class\":\"posts\",\"title\":\"title\",\"content\":\"content\",\"id\":\"a843d3f0-25fb-468c-8452-f14b1f0c6f42\",\"coll\":[1,2,3],\"tags\":[\"one\",\"two\",\"three\"],\"author\":{\"name\":\"swampie\",\"roles\":[\"Admin\",\"Consultant\"]}}";
		String data = "{\"data\":\"Java Developer\"}";
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		PartsLexer pl = new PartsLexer();
		parts.add(pl.parse(".author", 1));
		parts.add(pl.parse(".roles[2]", 2));
		PartsParser pp = new PartsParser(parts);
		ObjectMapper om = new ObjectMapper();
		JsonNode json = om.readTree(content);
		JsonNode jsonData = om.readTree(data);
		try{
			assertTrue(json.has("author"));
			assertTrue(json.get("author").has("roles"));
			assertEquals(2,json.get("author").get("roles").size());
			JsonTree.write(json, pp, jsonData.get("data"));
			assertEquals(3,json.get("author").get("roles").size());
			
		}catch(Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testComplexQuery() throws Exception{
		Random r = new Random();
		
		URL u = new URL("https://google-api-go-client.googlecode.com/hg-history/adc5d697472e6769364e3d60214e7251dcada595/drive/v2/drive-api.json?rand="+r.nextInt(50000));
         BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()));
         String line;
         StringBuffer content = new StringBuffer();
         while ((line = reader.readLine()) != null) {
             content.append(line);
         }
         reader.close();
         
         long start = System.nanoTime();
         
         ObjectMapper om = new ObjectMapper();
         JsonNode jn = om.readTree(content.toString());
         
         List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
 		PartsLexer pl = new PartsLexer();
 		parts.add(pl.parse(".resources", 1));
 		parts.add(pl.parse(".about", 2));
 		parts.add(pl.parse(".methods", 3));
 		parts.add(pl.parse(".get", 4));
 		parts.add(pl.parse(".scopes", 5));
 		PartsParser pp = new PartsParser(parts);
 		JsonNode res = JsonTree.read(jn, pp);
 		long end = System.nanoTime();
 		double seconds = (double)(end-start) / 1000000000.0;
 		assertTrue(seconds < 1.0);
 		assertTrue(res instanceof ArrayNode);
 		assertEquals("https://www.googleapis.com/auth/drive",res.get(0).asText());
		
		
	}
	
	@Test
	public void testComplexWrite() throws Exception{
		Random r = new Random();
		
		URL u = new URL("https://google-api-go-client.googlecode.com/hg-history/adc5d697472e6769364e3d60214e7251dcada595/drive/v2/drive-api.json?rand="+r.nextInt(50000));
         BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()));
         String line;
         StringBuffer content = new StringBuffer();
         while ((line = reader.readLine()) != null) {
             content.append(line);
         }
         reader.close();
         
         String data = "{\"data\":\"http://baasbox.com\"}";
         long start = System.nanoTime();
         ObjectMapper om = new ObjectMapper();
         JsonNode jn = om.readTree(content.toString());
         JsonNode jsonData = om.readTree(data);
         List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
 		PartsLexer pl = new PartsLexer();
 		parts.add(pl.parse(".resources", 1));
 		parts.add(pl.parse(".about", 2));
 		parts.add(pl.parse(".methods", 3));
 		parts.add(pl.parse(".get", 4));
 		parts.add(pl.parse(".scopes[4]", 5));
 		PartsParser pp = new PartsParser(parts);
 		JsonNode res = JsonTree.write(jn, pp,jsonData.get("data"));
 		long end = System.nanoTime();
 		double seconds = (double)(end-start) / 1000000000.0;
 		assertTrue(seconds < 1.0);
 		assertEquals("http://baasbox.com",res.get(4).asText());
 		
 		JsonNode result = jn.path("resources").path("about").path("methods").path("get").path("scopes");
 		assertTrue(result instanceof ArrayNode);
 		ArrayNode arrNode = (ArrayNode)result;
 		assertEquals(5,arrNode.size());
 		assertEquals(arrNode.get(4).asText(),res.get(4).asText());
 		
		
		
	}
	 



}
