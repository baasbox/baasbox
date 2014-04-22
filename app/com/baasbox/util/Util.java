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
package com.baasbox.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

import play.Logger;

public class Util {
	/**
	 * From the code of OrientDB OJSONWriter class, added fetchPlan as parameter
	 * @param iRecords
	 * @return
	 * @throws IOException
	 */
	/*
	  public static String listToJSON(Collection<? extends OIdentifiable> iRecords,String fetchPlan) throws IOException {
		  if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		    final StringWriter buffer = new StringWriter();
		    final OJSONWriter json = new OJSONWriter(buffer);
		    json.beginCollection(0, false, null);
		    if (iRecords != null) {
		      int counter = 0;
		      String objectJson;
		      for (OIdentifiable rec : iRecords) {
		        if (rec != null)
		          try {
		            objectJson = rec.getRecord().toJSON(fetchPlan);

		            if (counter++ > 0)
		              buffer.append(", ");

		            buffer.append(objectJson);
		          } catch (Throwable e) {
		            Logger.error("Error transforming record " + rec.getIdentity() + " to JSON", e);
		            throw e;
		          }
		      }
		    }
		    json.endCollection(0, false);
		    if (Logger.isTraceEnabled()) Logger.trace("Method End");
		    return buffer.toString();
		  }//listToJSON
	 */
	private static final String EMAIL_PATTERN = 
			"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
					+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

	public static boolean validateEmail(final String hex) {

		Pattern pattern = Pattern.compile(EMAIL_PATTERN);
		Matcher matcher = pattern.matcher(hex);
		return matcher.matches();

	}

	
	public static void createZipFile(String path,File...files) {
		if (Logger.isDebugEnabled()) Logger.debug("Zipping into:"+path);
		ZipOutputStream zip = null;
		FileOutputStream dest = null;
		try{
			File f = new File(path);
			dest = new FileOutputStream(f);
			zip = new ZipOutputStream(new BufferedOutputStream(dest));
			for (File file : files) {
				zip.putNextEntry(new ZipEntry(file.getName()));
				zip.write(FileUtils.readFileToByteArray(file));
				zip.closeEntry();
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("Unable to create zip file");
		}finally{
			try{
				if(zip!=null)
					zip.close();
				if(dest!=null)
					dest.close();

			}catch(Exception ioe){
				//Nothing to do
			}
		}
	}
}
