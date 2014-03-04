package com.baasbox.configuration.index;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexPushConfiguration extends IndexDao {
	private final static String indexName="_bb_push";
	
		public IndexPushConfiguration() throws IndexNotFoundException {
		super(indexName);
		// TODO Auto-generated constructor stub
	}

}
