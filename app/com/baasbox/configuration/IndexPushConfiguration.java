package com.baasbox.configuration;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexPushConfiguration extends IndexDao {
	private final static String indexName="bb_push";
	
		protected IndexPushConfiguration() throws IndexNotFoundException {
		super(indexName);
		// TODO Auto-generated constructor stub
	}

}
