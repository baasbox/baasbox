import com.baasbox.dao.ScriptsDao;
import com.baasbox.dao.exception.ScriptException;
import com.baasbox.db.DbHelper;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.scripting.ScriptingService;
import com.baasbox.service.scripting.base.ScriptLanguage;
import com.baasbox.service.scripting.js.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import play.Play;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.junit.Assert.fail;

/**
 * Created by eto on 30/09/14.
 */
public class ScriptTestHelpers {

    public static void createScript(String name,String source){
        try {
            DbHelper.open("1234567890","admin","admin");
            ScriptingService.create(ScriptTestHelpers.loadScript(name, source));

        } catch (ScriptException | InvalidAppCodeException e) {
            fail(ExceptionUtils.getFullStackTrace(e));
        } finally {
            DbHelper.close(DbHelper.getConnection());
        }
    }

    public static String randomScriptName(){
        return "script.test"+UUID.randomUUID().toString().replace("-","_");
    }


    public static JsonNode loadScript(String name,String source){
        try(InputStream in = Play.application().resourceAsStream(source)){
            String code = IOUtils.toString(in, Charset.defaultCharset());
            ObjectNode script = Json.mapper().createObjectNode();
            script.put(ScriptsDao.NAME,name);
            script.put(ScriptsDao.CODE,code);
            script.put(ScriptsDao.ACTIVE,true);
            script.put(ScriptsDao.LANG, ScriptLanguage.JS.name);
            script.put(ScriptsDao.LIB,false);
            return script;

        } catch (IOException e){
            Assert.fail(ExceptionUtils.getFullStackTrace(e));
            throw new RuntimeException(e); // really not thrown in the tests
        }
    }
}
