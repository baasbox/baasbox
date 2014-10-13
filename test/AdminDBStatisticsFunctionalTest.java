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

import static play.test.Helpers.GET;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import core.AbstractAdminTest;

public class AdminDBStatisticsFunctionalTest extends AbstractAdminTest
{
	@Override
	public String getRouteAddress()
	{
		return "/admin/dbStatistics";
	}
	
	@Override
	public String getMethod()
	{
		return GET;
	}
	
	@Override
	protected void assertContent(String sContent)
	{
		Object obj = toJSON(sContent);
		Object data = null;
		try {
			data = ((JSONObject)obj).get("data");
			Object installation = ((JSONObject)data).get("installation");
			Object db = ((JSONObject)data).get("db");
			Object dataInternal = ((JSONObject)data).get("data");
			Object os = ((JSONObject)data).get("os");
			Object java = ((JSONObject)data).get("java");
			Object memory = ((JSONObject)data).get("memory");
			
			assertJSON(installation, "bb_id");
			assertJSON(installation, "bb_version");
			
			assertJSON(db, "datafile_freespace");
			assertJSON(db, "physical_size");
			assertJSON(db, "size_threshold_percentage");
			
			assertJSON(dataInternal, "users");
			assertJSON(dataInternal, "collections");
			assertJSON(dataInternal, "collections_details");
			assertJSON(dataInternal, "assets");
			assertJSON(dataInternal, "files");
			
			assertJSON(os, "os_arch");
			assertJSON(os, "os_name");
			assertJSON(os, "os_version");
			assertJSON(os, "processors");
			
			assertJSON(java, "java_class_version");
			assertJSON(java, "java_vendor");
			assertJSON(java, "java_vendor_url");
			assertJSON(java, "java_version");
			
			assertJSON(memory, "max_allocable_memory");
			assertJSON(memory, "current_allocate_memory");
			assertJSON(memory, "used_memory_in_the_allocate_memory");
			assertJSON(memory, "free_memory_in_the_allocated_memory");
			
		} catch (JSONException e) {
			Assert.fail(ExceptionUtils.getFullStackTrace(e));
		}
	}
}
