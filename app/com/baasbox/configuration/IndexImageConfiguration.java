package com.baasbox.configuration;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexImageConfiguration extends IndexDao {
	private final static String indexName="_bb_images";
	
	public IndexImageConfiguration() throws IndexNotFoundException{
		super (indexName);
	}


	
}
