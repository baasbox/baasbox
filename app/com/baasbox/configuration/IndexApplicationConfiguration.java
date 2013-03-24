	package com.baasbox.configuration;

	import com.baasbox.dao.IndexDao;
	import com.baasbox.exception.IndexNotFoundException;

public class IndexApplicationConfiguration extends IndexDao{

		private final static String indexName="bb_application";
		
		public IndexApplicationConfiguration() throws IndexNotFoundException{
			super (indexName);
		}


}
