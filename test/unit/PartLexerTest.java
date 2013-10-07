package unit;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import play.libs.Json;

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
		String jn = parser.generateUpdateFields();
		
		
	}
	
}
