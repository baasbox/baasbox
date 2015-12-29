/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

// @author: Marco Tibuzzi

import static play.test.Helpers.DELETE;

import java.io.InputStream;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.baasbox.db.DbHelper;
import com.baasbox.service.storage.FileService;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

import core.AbstractAdminAssetTest;
import play.Play;
import play.test.FakeApplication;
import play.test.Helpers;


//issue 914: Enormous default.pcl file size 
//https://github.com/baasbox/baasbox/issues/914

//When a File is deleted, the datafile and other related data must be deleted from the Database


public class FileDeleteTest extends AbstractAdminAssetTest
{
	@Override
	public String getRouteAddress()
	{
		return "/file/fakeFileId";
	}
	
	@Override
	public String getMethod()
	{
		return DELETE;
	}

	@Override
	protected void assertContent(String s)
	{
	}
	
	@Test
	public void testImageDeletion() throws Throwable{
		FakeApplication fa = getFakeApplicationWithDefaultConf();
		try{
			Helpers.start(fa);
			//open a connection
			try (ODatabaseRecordTx db=DbHelper.open("1234567890", "admin", "admin")){
				//create a file
				InputStream is = Play.application().resourceAsStream("/logo_baasbox_lp.png");
				ODocument createdFile = FileService.createFile("test_file_to_delete.png", "{\"key\":\"this is some data\"}", "image/png", 70673, is);
				String idFile = createdFile.field("id");
				//resize the image to create more blobs on db
				FileService.getResizedPicture(idFile, 1);
				createdFile=(ODocument)createdFile.reload();
				System.out.println(createdFile);
				//check related data
				ORID createdFileRid = createdFile.getIdentity();
				ODocument createdFileLinks = ((ODocument)createdFile.field("_links"));
				ORecordBytes createdFileBlob = createdFile.field("file");
				ODocument createdAudit = ((ODocument)createdFile.field("_audit"));
				ORecordBytes createdFileBlobResized = (ORecordBytes)((Map)createdFile.field("resized")).get("50%-50%");
				//delete the file
				FileService.deleteById(idFile);
				
				//check related data
				Assert.assertTrue("The file has not been deleted. Its RID is " + createdFile.getIdentity(),
						db.load(createdFileRid)==null);
				Assert.assertTrue("The _links vertex has not been deleted. File RID was : " + createdFile.getIdentity() + " , _links RID is: " + createdFileLinks.getIdentity(),
						db.load(createdFileLinks.getIdentity())==null);				
				Assert.assertTrue("The BLOB has not been deleted. File RID was : " + createdFile.getIdentity() + " , blob RID is: " + createdFileBlob.getIdentity(),
						db.load(createdFileBlob.getIdentity())==null);
				Assert.assertTrue("The RESIZED BLOB has not been deleted. File RID was : " + createdFile.getIdentity() + " , resized blob RID is: " + createdFileBlobResized.getIdentity(),
						db.load(createdFileBlobResized.getIdentity())==null);
				//close connection (implicitly done by the try () statement)
			}finally{} 
		}finally{
			Helpers.stop(fa);
		}
		
	}
}
