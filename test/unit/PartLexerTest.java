package unit;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertEquals(jn, "field0.field1.field2");
		Assert.assertEquals(parser.last().getName(), "field2");
		Assert.assertEquals(parser.lastParent().getName(), "field1");
		Assert.assertEquals(parser.first().getName(), "field0");
		
		
	}
	
	@Test
	public void testUpdateStringOnlyOneField(){
		List<PartsLexer.Part> parts = new ArrayList<PartsLexer.Part>();
		parts.add(PartFactory.parse(".field0",1));
		PartsParser parser = new PartsParser(parts);
		String jn = parser.treeFields();
		Assert.assertEquals("field0", jn);
		Assert.assertEquals(parser.last().getName(), "field0");
		Assert.assertEquals(parser.lastParent().getName(), "field0");
		Assert.assertEquals(parser.first().getName(), "field0");
		
		
	}
	
}
