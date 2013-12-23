package com.baasbox.service.user;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.baasbox.dao.RoleDao;
import com.baasbox.dao.UserDao;
import com.baasbox.dao.exception.InvalidCriteriaException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class FriendShipService {
	public static final String FRIEND_ROLE_NAME=RoleDao.FRIENDS_OF_ROLE;
	public static final String WHERE_FRIENDS="(" + UserDao.USER_LINK + ".roles contains (name=?))";
	
	private static String getWhereFromCriteria(QueryParams criteria){
		String where=WHERE_FRIENDS;
		if (!StringUtils.isEmpty(criteria.getWhere())) where += " and (" + criteria.getWhere() + ")";
		return where;
	}
	
	private static Object[] addFriendShipRoleToCriteria(QueryParams criteria, String friendShipRole){
		Object[] params = criteria.getParams();
		Object[] newParams = new Object[] {friendShipRole};
		Object[] veryNewParams = ArrayUtils.addAll(newParams, params);
		return veryNewParams;
	}
	
	public static List<ODocument> getFriendsOf(String username, QueryParams criteria) throws InvalidCriteriaException, SqlInjectionException {
		String friendShipRole=RoleDao.getFriendRoleName(username);
		criteria.where(getWhereFromCriteria(criteria));
		criteria.params(addFriendShipRoleToCriteria(criteria, friendShipRole));
		UserDao udao= UserDao.getInstance();
		return udao.get(criteria);
	}

	public static long getCountFriendsOf(String username, QueryParams criteria) throws InvalidCriteriaException, SqlInjectionException {
		String friendShipRole=RoleDao.getFriendRoleName(username);
		criteria.where(getWhereFromCriteria(criteria));
		criteria.params(addFriendShipRoleToCriteria(criteria, friendShipRole));
		UserDao udao= UserDao.getInstance();
		return udao.getCount(criteria);
	}
}
