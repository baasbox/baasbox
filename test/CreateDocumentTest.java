

import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import com.baasbox.service.storage.CollectionService;
import com.baasbox.service.storage.DocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.exception.OSerializationException;

import core.AbstractTest;

public class CreateDocumentTest  extends AbstractTest{
	
	@Test
	public void versionNull() throws Throwable {
		JsonNode bodyJson;
		String collection=(new AdminCollectionFunctionalTest()).routeCreateCollection();
		CollectionService.create(collection);
		bodyJson= (new ObjectMapper()).readTree("{\"@version\":null}");
		try{
			DocumentService.create(collection, bodyJson);
		}catch (OSerializationException e){
			fail();
		}
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
