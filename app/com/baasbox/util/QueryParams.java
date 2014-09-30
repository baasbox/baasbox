/*
     Copyright 2012-2013 
     Claudio Tesoriero - c.tesoriero-at-baasbox.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.baasbox.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import com.baasbox.BBConfiguration;

import play.Logger;


public class QueryParams implements IQueryParametersKeys{
	private boolean justCount=false;
	private String fields="";
	private String where="";
	private Integer page=-1;
	private Integer skip=null;
	private Integer recordPerPage=new Integer(BBConfiguration.configuration.getString(BBConfiguration.QUERY_RECORD_PER_PAGE));
	private String groupBy="";
	private String orderBy="";
	private Integer depth=new Integer(BBConfiguration.configuration.getString(BBConfiguration.QUERY_RECORD_DEPTH));;
	private Object[] params={};


	protected QueryParams(){};
	
	protected QueryParams(String where, Integer page, Integer recordPerPage,
			String orderBy, Integer depth, String[] params) {
		super();
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (where!=null) this.where = where;
		if (page!=null) this.page = page;
		if (recordPerPage!=null) this.recordPerPage = recordPerPage;
		if (orderBy!=null) this.orderBy = orderBy;
		if (depth!=null) this.depth = depth;
		if (params!=null) this.params=params;
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	}

	
	protected QueryParams(String where, Integer page, Integer recordPerPage,
			String orderBy, Integer depth, String param) {
		super();
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
		if (where!=null) this.where = where;
		if (page!=null) this.page = page;
		if (recordPerPage!=null) this.recordPerPage = recordPerPage;
		if (orderBy!=null) this.orderBy = orderBy;
		if (depth!=null) this.depth = depth;
		if (param!=null) {
			String[] params = {param};
			this.params= params ;
		}
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
	}
	
	public QueryParams(String fields, String where, Integer page,
			Integer recordPerPage, String orderBy, Integer depth,
			String[] params) {
		this(where,  page,recordPerPage,  orderBy,  depth,	 params);
		if (fields!=null) this.fields = fields;
	}

	public QueryParams(String fields, String where, Integer page,
			Integer recordPerPage, String orderBy, Integer depth,
			String params) {
		this(where,  page,recordPerPage,  orderBy,  depth,	 params);
		if (fields!=null) this.fields = fields;
	}
	
	public QueryParams(String fields, String groupBy, String where,
			Integer page, Integer recordPerPage, String orderBy,
			Integer depth, String[] params) {
		this(fields,where,  page,recordPerPage,  orderBy,  depth,	 params);
		if (groupBy!=null) this.groupBy = groupBy;
	}

	public QueryParams(String fields, String groupBy, String where,
			Integer page, Integer recordPerPage, String orderBy,
			Integer depth, String params) {
		this(fields,where,  page,recordPerPage,  orderBy,  depth,	 params);
		if (groupBy!=null) this.groupBy = groupBy;
	}
	
	protected QueryParams(String fields, String groupBy, String where,
			Integer page, Integer recordPerPage, String orderBy,
			Integer depth, String[] params,Boolean count) {
		this(fields, groupBy, where,  page,recordPerPage,  orderBy,  depth, params);
		this.justCount=count!=null?count:false;
	}
	
	protected QueryParams(String fields, String groupBy, String where,
			Integer page, Integer recordPerPage, String orderBy,
			Integer depth, String[] params,Boolean count,Integer skip) {
		this(fields, groupBy, where,  page,recordPerPage,  orderBy,  depth, params,count);
		this.skip=skip!=null?skip:null;
	}

	public boolean justCountTheRecords(){
		return justCount;
	}
	
	/**
	 * @return the where
	 */
	public String getWhere() {
		return where;
	}

	/**
	 * @return the page
	 */
	public Integer getPage() {
		return page;
	}

	/**
	 * @return the recordPerPage
	 */
	public Integer getRecordPerPage() {
		return recordPerPage;
	}

	/**
	 * @return the orderBy
	 */
	public String getOrderBy() {
		return orderBy;
	}

	/**
	 * @return the groupBy
	 */
	public String getGroupBy() {
		return groupBy;
	}
	
	/**
	 * @return the depth
	 */
	public Integer getDepth() {
		return depth;
	}

	/**
	 * @return the number of records to skip
	 */
	public Integer getSkip() {
		return skip;
	}
	
	/**
	 * @return the params
	 */
	public Object[] getParams() {
		return params;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "QueryParams ["
				+ ("justCount="+justCount +", ")
				+ (fields != null ? "fields=" + fields + ", " : "")
				+ (where != null ? "where=" + where + ", " : "")
				+ (params != null ? "params=" + Arrays.toString(params) + ", ": "")
				+ (page != null ? "page=" + page + ", " : "")
				+ (recordPerPage != null ? "recordPerPage=" + recordPerPage + ", " : "")
				+ (skip != null ? "skip=" + skip + ", " : "")
				+ (groupBy != null ? "groupBy=" + groupBy + ", " : "")		
				+ (orderBy != null ? "orderBy=" + orderBy + ", " : "")
				+ (depth != null ? "depth=" + depth + " " : "")
				+ "]";
	}

	public QueryParams fields (String fields){
		if (fields!=null)
			this.fields=fields;
		return this;
	}
	
	public QueryParams groupBy (String groupBy){
		if (groupBy!=null)
			this.groupBy=groupBy;
		return this;
	}
	
	public QueryParams where (String whereCondition){
		if (whereCondition!=null)
			this.where=whereCondition;
		return this;
	}

	public QueryParams orderBy (String orderBy){
		if (orderBy!=null)
			this.orderBy=orderBy;
		return this;
	}
	
	public QueryParams params (String[] params){
		if (params!=null)
			this.params=params;
		return this;
	}
	
	public QueryParams params (Object[] params){
		if (params!=null)
			this.params=params;
		return this;
	}
	
	public  QueryParams justCountTheRecords(boolean justCount){
		this.justCount=justCount;
		return this;
	}
	
	public  QueryParams skip(Integer skip){
		this.skip=skip;
		return this;
	}
	
	public static QueryParams getInstance(){
		return new QueryParams();
	}


    public static QueryParams getParamsFromJson(JsonNode node){
        if (node == null) {
            return QueryParams.getInstance();
        }
        Map<String,String[]> query = new HashMap<String,String[]>();
        Iterator<Map.Entry<String, JsonNode>> nodes = node.fields();
        while (nodes.hasNext()) {
            Map.Entry<String, JsonNode> next = nodes.next();
            String k = next.getKey();
            JsonNode val = next.getValue();
            if (val.isArray()) {
                String[] ary = new String[val.size()];
                int idx = 0;
                for (JsonNode n: val){
                    String s = n==null?null:n.toString();
                    ary[idx] = s;
                }
                query.put(k,ary);
            } else {
                String[] o = {val.asText()};
                query.put(k,o);
            }
        }

        return getParamsFromQueryString(query);
    }

    public static QueryParams getParamsFromQueryString(play.mvc.Http.RequestHeader header){
        return getParamsFromQueryString(header.queryString());
    }

	
	public static QueryParams getParamsFromQueryString(Map<String,String[]> queryString){
		String fields;
		String where;
		Integer page;
		Integer recordPerPage;
		String groupBy;
		String orderBy;
		Integer depth;
		Boolean count;
		Integer skip;
		String[] params;

		String fieldsFromQS=null;
		String whereFromQS=null;
		String pageFromQS=null;
		String recordPerPageFromQS=null;
		String orderByFromQS=null;
		String groupByFromQS=null;
		String depthFromQS=null;
		String countFromQS=null;
		String skipFromQS=null;
		
		if (Logger.isTraceEnabled()) Logger.trace("Method Start");
//		Map <String,String[]> queryString = header.queryString();
		if (queryString.get(IQueryParametersKeys.FIELDS)!=null)
			fieldsFromQS=queryString.get(IQueryParametersKeys.FIELDS)[0];
		if (queryString.get(IQueryParametersKeys.WHERE)!=null)
			whereFromQS=queryString.get(IQueryParametersKeys.WHERE)[0];
		if (queryString.get(IQueryParametersKeys.PAGE)!=null)
			pageFromQS=queryString.get(IQueryParametersKeys.PAGE)[0];
		if (queryString.get(IQueryParametersKeys.RECORD_PER_PAGE)!=null)
			recordPerPageFromQS=queryString.get(IQueryParametersKeys.RECORD_PER_PAGE)[0];
		if (queryString.get(IQueryParametersKeys.RECORDS_PER_PAGE)!=null)
			recordPerPageFromQS=queryString.get(IQueryParametersKeys.RECORDS_PER_PAGE)[0];
		if (queryString.get(IQueryParametersKeys.ORDER_BY)!=null)
			orderByFromQS=queryString.get(IQueryParametersKeys.ORDER_BY)[0];
		if (queryString.get(IQueryParametersKeys.GROUP_BY)!=null)
			groupByFromQS=queryString.get(IQueryParametersKeys.GROUP_BY)[0];
		if (queryString.get(IQueryParametersKeys.DEPTH)!=null)
			depthFromQS=queryString.get(IQueryParametersKeys.DEPTH)[0];
		if (queryString.get(IQueryParametersKeys.COUNT)!=null)
			countFromQS=queryString.get(IQueryParametersKeys.COUNT)[0];
		if (queryString.get(IQueryParametersKeys.SKIP)!=null)
			skipFromQS=queryString.get(IQueryParametersKeys.SKIP)[0];
		
		params = queryString.get(IQueryParametersKeys.PARAMS);
		
		fields=fieldsFromQS;
		where=whereFromQS;
		groupBy=groupByFromQS;
		orderBy=orderByFromQS;
		try{
			page=pageFromQS==null?null:new Integer(pageFromQS);
		}catch (NumberFormatException e){
			throw new NumberFormatException(IQueryParametersKeys.PAGE + " parameter must be a valid Integer");
		}
		try{
			recordPerPage=recordPerPageFromQS==null?null:new Integer(recordPerPageFromQS);
		}catch (NumberFormatException e){
			throw new NumberFormatException(IQueryParametersKeys.RECORDS_PER_PAGE + " parameter must be a valid Integer");
		}
		try{
			depth=depthFromQS==null?null:new Integer(depthFromQS);
		}catch (NumberFormatException e){
			throw new NumberFormatException(IQueryParametersKeys.DEPTH + " parameter must be a valid Integer");
		}		
		try{
			count=countFromQS==null?null:new Boolean(countFromQS);
		}catch (NumberFormatException e){
			throw new NumberFormatException(IQueryParametersKeys.COUNT + " parameter must be true or false");
		}	
		try{
			skip=skipFromQS==null?null:new Integer(skipFromQS);
		}catch (NumberFormatException e){
			throw new NumberFormatException(IQueryParametersKeys.SKIP + " parameter must be a valid Integer");
		}	
		QueryParams qryp = new QueryParams(fields,groupBy,where, page, recordPerPage, orderBy, depth,params,count,skip);
		
		if (Logger.isTraceEnabled()) Logger.trace("Method End");
		return qryp;
		
	}

	public String getFields() {
		return fields;
	}
}
