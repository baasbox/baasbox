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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;

import play.Logger;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;

public class Util {
	/**
	 * From the code of OrientDB OJSONWriter class, added fetchPlan as parameter
	 * @param iRecords
	 * @return
	 * @throws IOException
	 */
	/*
	  public static String listToJSON(Collection<? extends OIdentifiable> iRecords,String fetchPlan) throws IOException {
		  Logger.trace("Method Start");
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
		    Logger.trace("Method End");
		    return buffer.toString();
		  }//listToJSON
		  */
}
