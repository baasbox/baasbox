package com.baasbox.configuration.index;

import com.baasbox.dao.IndexDao;
import com.baasbox.exception.IndexNotFoundException;

public class IndexPasswordRecoveryConfiguration extends IndexDao {
	private final static String indexName="_bb_password_recovery";
	
	public IndexPasswordRecoveryConfiguration() throws IndexNotFoundException{
		super (indexName);
	}


	
}
