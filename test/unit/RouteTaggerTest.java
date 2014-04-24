package unit;

import com.baasbox.service.permissions.RouteTagger;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Map;
import java.util.Set;

/**
 * Created by eto on 08/04/14.
 */
public class RouteTaggerTest {

    @Test
    public void emptyComment(){
        Map<String, Set<String>> parsed = RouteTagger.parse("");
        assertTrue(parsed.isEmpty());
    }

    @Test
    public void nullComment(){
        Map<String, Set<String>> parse = RouteTagger.parse(null);
        assertTrue(parse.isEmpty());
    }

    @Test
    public void garbageComment(){
        Map<String, Set<String>> parse = RouteTagger.parse("dasndainasas dnasodnas @ dasda ()\n@ xxx blah @2x");
        assertTrue(parse.isEmpty());
    }

    @Test
    public void simpleAnnotation(){
        Map<String, Set<String>> parse = RouteTagger.parse("@simple");
        Set<String> annotation = parse.get("simple");
        assertNotNull(annotation);
        assertTrue(annotation.contains("simple"));
        assertEquals(1,annotation.size());
    }

    @Test
    public void parametricAnnotation(){
        Map<String, Set<String>> parsed = RouteTagger.parse("@parametric(val)");
        Set<String> annotations = parsed.get("parametric");
        assertNotNull(annotations);
        assertTrue(annotations.contains("val"));
        assertEquals(1,annotations.size());
    }

}
