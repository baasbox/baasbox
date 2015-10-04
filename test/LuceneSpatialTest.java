import static play.test.Helpers.running;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.baasbox.util.BBJson;
import com.baasbox.util.QueryParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.record.impl.ODocument;

import core.AbstractTest;


public class LuceneSpatialTest extends AbstractTest {

	private ObjectNode document1;
	private ObjectNode document2;
	private ObjectNode document3;
	private ObjectNode document4;
	
	
	@Before
	public void createCollection() throws JsonProcessingException, IOException{
		document1 = (ObjectNode) BBJson.mapper().readTree("{\"city\":\"rome\",\"lat\": 41.9,\"long\": 12.416667}");
		document2 = (ObjectNode) BBJson.mapper().readTree("{\"city\":\"milan\",\"lat\": 45.46427,\"long\": 9.18951}");
		document3 = (ObjectNode) BBJson.mapper().readTree("{\"city\":\"naples\",\"lat\": 40.85631,\"long\": 14.24641}");
		document4 = (ObjectNode) BBJson.mapper().readTree("{\"city\":\"florence\",\"lat\": 43.77925,\"long\": 11.24626}");
	}
	
	@Test
	public void test(){
		running
		(
				getFakeApplication(), 
			new Runnable() 		{
				public void run() {
			 		continueOnFail(false);
					String sFakeCollection = "spatial_" + UUID.randomUUID().toString();
					try {
						DbHelper.open("1234567890", "admin", "admin");
						CollectionService.create(sFakeCollection);
						DocumentService.create(sFakeCollection, document1);
						DocumentService.create(sFakeCollection, document2);
						DocumentService.create(sFakeCollection, document3);
						DocumentService.create(sFakeCollection, document4);
						
						DbHelper.execMultiLineCommands(DbHelper.getConnection(), false, new String[]{
							"create property "+sFakeCollection+".lat double",
							"create property "+sFakeCollection+".long double",
							"create index "+sFakeCollection+".lat_long on "+sFakeCollection+" (lat,long) SPATIAL ENGINE LUCENE"
						});
						
						QueryParams criteria = QueryParams.getInstance().fields("city,$distance")
												.where("[lat,long,$spatial] NEAR [41.9, 12.416667, {\"maxDistance\":200}]")
												.orderBy("$distance desc");
						List<ODocument> result = DocumentService.getDocuments(sFakeCollection, criteria);
						Assert.assertFalse("Result of the query is empty!",result.isEmpty());
						Assert.assertTrue("Result of the query is not 2. Actually is " + result.size(),result.size()==2);
						Assert.assertTrue("First record is not naples. Actually is " + ((String)result.get(0).field("city")), ((String)result.get(0).field("city")).equals("naples"));
						Assert.assertTrue("Second record is not rome. Actually is " + ((String)result.get(0).field("city")), ((String)result.get(1).field("city")).equals("rome"));
						
						ODocument o = (ODocument) DbHelper.genericSQLStatementExecute("explain select from " + sFakeCollection + " where [lat,long,$spatial] NEAR [41.9, 12.416667, {\"maxDistance\":200}]", null);
						Set ind=(Set) o.field("involvedIndexes");
						Assert.assertTrue("No indexes has been used",ind.size()>0);
						ind.stream().forEach(x->{
							Assert.assertTrue("the SPATIAL index was not used by the db engine",x.equals(sFakeCollection+".lat_long"));
						});
					}catch (Throwable e){
						DbHelper.close(DbHelper.getConnection());
						assertFail(ExceptionUtils.getFullStackTrace(e));
					}
										
				}
			}
		);			
	}

	@Override
	public String getRouteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void assertContent(String s) {
		// TODO Auto-generated method stub
		
	}
	
	
}


