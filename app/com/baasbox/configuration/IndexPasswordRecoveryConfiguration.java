package com.baasbox.configuration;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexPasswordRecoveryConfiguration extends IndexDao {
	private final static String indexName="bb_password_recovery";
	
	public IndexPasswordRecoveryConfiguration() throws IndexNotFoundException{
		super (indexName);
	}


	
}
