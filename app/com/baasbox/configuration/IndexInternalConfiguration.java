	package com.baasbox.configuration;

	import com.baasbox.dao.IndexDao;
	import com.baasbox.exception.IndexNotFoundException;

public class IndexInternalConfiguration extends IndexDao{

		private final static String indexName="_bb_internal";
		
		public IndexInternalConfiguration() throws IndexNotFoundException{
			super (indexName);
		}


}
