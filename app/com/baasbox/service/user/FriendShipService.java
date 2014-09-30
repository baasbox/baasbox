/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.service.user;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.baasbox.BBConfiguration;
import com.baasbox.exception.AlreadyFriendsException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.exception.UserToFollowNotExistsException;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
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

    public static List<ODocument> getFollowing(String username,QueryParams criteria) throws SqlInjectionException {
        OUser me = UserService.getOUserByUsername(username);
        Set<ORole> roles = me.getRoles();
        List<String> usernames = roles.parallelStream().map(ORole::getName)
                .filter((x) -> x.startsWith(RoleDao.FRIENDS_OF_ROLE))
                .map((m) -> StringUtils.difference(RoleDao.FRIENDS_OF_ROLE, m))
                .collect(Collectors.toList());
        if (username.isEmpty()){
            return Collections.emptyList();
        } else {
            List<ODocument> followers = UserService.getUserProfileByUsernames(usernames,criteria);
            return followers;
        }

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

    public static boolean unfollow(String from,String to) throws UserNotFoundException,Exception{
        OUser fromUser = UserService.getOUserByUsername(from);
        if (fromUser == null){
            throw new UserNotFoundException("User "+from+" does not exists");
        }
        if (!UserService.exists(to)){
            throw new UserToFollowNotExistsException("User "+to+" does not exists");
        }
        String friendshipName = RoleDao.FRIENDS_OF_ROLE+to;
        boolean areFriends = RoleService.hasRole(fromUser.getName(),friendshipName);
        if (areFriends) {
            UserService.removeUserFromRole(fromUser.getName(),friendshipName);
            return true;
        } else {
            return false;
        }
    }

    public static ODocument follow(String from,String to) throws UserNotFoundException, AlreadyFriendsException,SqlInjectionException,Exception {
        if (UserService.isInternalUsername(to)){
            throw new IllegalArgumentException("Cannot follow internal users");
        }

        if (Objects.equals(from, to)){
            throw new IllegalArgumentException("User cannot follow himself");
        }
        OUser fromUser = UserService.getOUserByUsername(from);
        if (fromUser == null) {
             throw new UserNotFoundException("User " + from + " does not exists.");
        }

        boolean exists = UserService.exists(to);
        if (!exists){
            throw new UserToFollowNotExistsException("User "+to+" does not exists.");
        }

        String friendshipRoleName = RoleDao.FRIENDS_OF_ROLE+to;
        boolean isFriend = RoleService.hasRole(from,friendshipRoleName);
        if (isFriend){
            throw new AlreadyFriendsException("User "+fromUser.getName()+ " is already a friend of "+to);
        }
        UserService.addUserToRole(fromUser.getName(),friendshipRoleName);
        ODocument toUser = UserService.getUserProfilebyUsername(to);
        return toUser;
    }
}
