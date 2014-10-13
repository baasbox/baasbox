package unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.baasbox.service.query.PartsLexer;
import com.baasbox.service.query.PartsLexer.ArrayField;
import com.baasbox.service.query.PartsLexer.Field;
import com.baasbox.service.query.PartsLexer.PartValidationException;
import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

public class PartParserTest {

	PartsLexer parser = new PartsLexer();
	Pattern fieldOrArray = Pattern.compile("^(?<name>(\\w+))(\\[(?<arrayIndex>(0|(?!0)\\d+))\\])?$");
	
	@Test
	public void testFieldRegex(){
		String field = "field";
		Matcher m = fieldOrArray.matcher(field);
		assertTrue(m.matches());
		assertEquals(field,m.group("name"));
	}
	
	@Test
	public void testFieldUnderscoreRegex(){
		String field = "_field";
		Matcher m = fieldOrArray.matcher(field);
		assertTrue(m.matches());
		assertEquals(field,m.group("name"));
	}
	
	
	@Test
	public void testSimpleField() throws Exception{
		try{
			String field = ".field";
			PartsLexer.Part p = parser.parse(field, 1);
			assertTrue(p instanceof Field);
			assertEquals("field",((Field)p).fieldName);
		}catch(PartValidationException pve){
			fail(pve.getMessage());
		}
		
	}
	
	@Test
	public void testWrongField() throws Exception{
		try{
			String field = ".fi.eld";
			parser.parse(field, 1);
			fail();
		}catch(Exception pve){
			assertTrue(pve instanceof PartValidationException);
			assertTrue(pve.getMessage().toLowerCase().indexOf("unrecognized")>-1);
		}
		
	}
	
	@Test
	public void testPrivateFieldId() throws Exception{
		try{
			String field = ".id";
			parser.parse(field, 1);
			fail();
		}catch(Exception pve){
			assertTrue(pve instanceof PartValidationException);
			assertTrue(pve.getMessage().toLowerCase().indexOf("private")>-1);
		}
		
	}
	
	@Test
	public void testPrivateFieldUnderscore() throws Exception{
		try{
			String field = "._audit";
			parser.parse(field, 1);
			fail();
		}catch(Exception pve){
			assertTrue(pve instanceof PartValidationException);
			assertTrue(pve.getMessage().toLowerCase().indexOf("private")>-1);
		}
		
	}
	
	@Test
	public void testArrayField() throws Exception{
		try{
			String field = ".field[0]";
			PartsLexer.Part p = parser.parse(field, 1);
			assertTrue(p instanceof ArrayField);
			assertEquals("field",((ArrayField)p).fieldName);
			assertEquals(0,((ArrayField)p).arrayIndex);
		}catch(PartValidationException pve){
			fail(pve.getMessage());
		}
		
	}
	
	@Test
	public void testArrayFieldLeadingZero1() throws Exception{
		try{
			String field = ".field[01]";
			PartsLexer.Part p = parser.parse(field, 1);
			assertEquals(1,((ArrayField)p).arrayIndex);
		}catch(PartValidationException pve){
			fail(pve.getMessage());
		}
		
	}
	
	@Test
	public void testArrayFieldLeadingZero2() throws Exception{
		try{
			String field = ".field[0000001042]";
			PartsLexer.Part p = parser.parse(field, 1);
			assertEquals(1042,((ArrayField)p).arrayIndex);
		}catch(PartValidationException pve){
			fail(pve.getMessage());
		}
		
	}
}
