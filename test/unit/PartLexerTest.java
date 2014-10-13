package unit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import com.baasbox.service.query.PartFactory;
import com.baasbox.service.query.PartsLexer;
import com.baasbox.service.query.PartsParser;

public class PartLexerTest {
	
	
	@Test
	public void testUpdateString(){
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		for(int i = 0;i<3;i++){
			parts.add(PartFactory.parse(".field"+i, i+1));
		}
		PartsParser parser = new PartsParser(parts);
		String jn = parser.treeFields();
		assertEquals(jn, "field0.field1.field2");
		assertEquals(parser.last().getName(), "field2");
		assertEquals(parser.lastParent().getName(), "field1");
		assertEquals(parser.first().getName(), "field0");
		
		
	}
	
	
	@Test
	public void testPrivateFieldError(){
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		try{
			parts.add(PartFactory.parse(".id",1));
			new PartsParser(parts);
			fail();
		}catch(Exception e ){
			assertEquals(PartsLexer.PartValidationException.class,e.getClass());
			assertTrue(e.getMessage().indexOf("private")>-1);
		}
	}
	
	@Test
	public void testPrivateFieldWithAtError(){
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		try{
			parts.add(PartFactory.parse(".@id",1));
			new PartsParser(parts);
			fail();
		}catch(Exception e ){
			assertEquals(PartsLexer.PartValidationException.class,e.getClass());
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().toLowerCase().indexOf("unrecognized")>-1);
		}
	}
	
	@Test
	public void testUpdateStringOnlyOneField(){
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		parts.add(PartFactory.parse(".field0",1));
		PartsParser parser = new PartsParser(parts);
		String jn = parser.treeFields();
		assertEquals("field0", jn);
		assertEquals(parser.last().getName(), "field0");
		assertEquals(parser.lastParent().getName(), "field0");
		assertEquals(parser.first().getName(), "field0");
		
		
	}
	
}
