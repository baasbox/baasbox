	package com.baasbox.configuration.index;

	import com.baasbox.dao.IndexDao;
	import com.baasbox.exception.IndexNotFoundException;

public class IndexApplicationConfiguration extends IndexDao{

		private final static String indexName="_bb_application";
		
		public IndexApplicationConfiguration() throws IndexNotFoundException{
			super (indexName);
		}


}
