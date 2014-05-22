/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.storage;

import java.util.List;

import com.baasbox.dao.LinkDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class LinkService {

	public static ODocument createLink(String sourceId, String destId,String edgeName) throws DocumentNotFoundException {
		return LinkDao.getInstance().createLink(sourceId, destId, edgeName);
	}
	
	public static ODocument getLink(String linkId) {
		return LinkDao.getInstance().getLink(linkId); 
	}
	
	public static List<ODocument> getLink(QueryParams criteria) throws SqlInjectionException {
		return LinkDao.getInstance().getLinks(criteria);
	}

	public static void deleteLink(String linkId) {
		LinkDao.getInstance().deleteLink(linkId); 
	}

}
