package com.baasbox.configuration;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexSocialLoginConfiguration extends IndexDao {
	private final static String indexName="_bb_social_login";
	
	public IndexSocialLoginConfiguration() throws IndexNotFoundException{
		super (indexName);
	}
}
