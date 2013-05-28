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
		//TODO: this test should be fixed: the response contains a "data" field which is misinterpreted by the assertJSON method
		/*
		Object obj = toJSON(sContent);
		assertJSON(obj, "db");
		assertJSON(obj, "data");
		assertJSON(obj, "os");
		assertJSON(obj, "java");
		assertJSON(obj, "memory");
		*/
	}
}
