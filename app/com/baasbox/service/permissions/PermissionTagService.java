package com.baasbox.service.permissions;

import com.baasbox.dao.PermissionTagDao;
import com.baasbox.dao.exception.InvalidPermissionTagException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.record.impl.ODocument;
import play.Logger;

import java.util.List;
import java.util.Set;

/**
 *
 * Created by Andrea Tortorella on 08/04/14.
 */
public class PermissionTagService {

    public static boolean areAllTagsEnabled(Set<String> tags) throws InvalidPermissionTagException, SqlInjectionException {
        if (tags==null||tags.isEmpty()) return true;
        PermissionTagDao dao = PermissionTagDao.getInstance();
        for (String tag:tags){
            if (!dao.isEnabled(tag)){
                return false;
            }
        }
        return true;
    }

    public static boolean isAtLeastOneTagEnabled(Set<String> tags) throws InvalidPermissionTagException, SqlInjectionException {
        if (tags==null||tags.isEmpty()) return true;
        PermissionTagDao dao=PermissionTagDao.getInstance();
        for (String tag:tags){
            if (dao.isEnabled(tag)) return true;
        }
        return false;
    }

    public static boolean isTagEnabled(String tag) throws InvalidPermissionTagException, SqlInjectionException {
        if (tag==null) return true;
        PermissionTagDao dao = PermissionTagDao.getInstance();
        return dao.isEnabled(tag);
    }

    public static boolean setTagEnabled(String tag,boolean enabled) throws InvalidPermissionTagException, SqlInjectionException {
        PermissionTagDao dao = PermissionTagDao.getInstance();
        return dao.setEnabled(tag,enabled);
    }

    public static List<ODocument> getPermissionTags(){
        PermissionTagDao dao = PermissionTagDao.getInstance();
        return dao.getAll();
    }

    public static ImmutableMap<String,Boolean> getPermissionTagsMap(){
        List<ODocument> tags = getPermissionTags();
        ImmutableMap.Builder<String,Boolean> map = ImmutableMap.builder();
        for (ODocument doc:tags){
            String name = doc.field(PermissionTagDao.TAG);
            boolean enabled = doc.field(PermissionTagDao.ENABLED);
            map.put(name,enabled);
        }
        return map.build();
    }

    public static void createDefaultPermissions(){
        PermissionTagDao dao = PermissionTagDao.getInstance();
        for (Tags.Reserved tag:Tags.Reserved.values()){
            try {
                dao.createReserved(tag.name);
            } catch (Throwable throwable) {
                if (Logger.isErrorEnabled()) Logger.error("Error while creating defaults tags");
                throw new RuntimeException(throwable);
            }
        }
    }

    public static ImmutableMap<String,Object> getPermissionTagMap(String name) throws SqlInjectionException{
        ODocument doc = getPermissionTag(name);
        if (doc == null) return null;
        String tag = doc.field(PermissionTagDao.TAG);
        boolean enabled = doc.field(PermissionTagDao.ENABLED);
        return ImmutableMap.<String,Object>of(PermissionTagDao.TAG,tag,PermissionTagDao.ENABLED,enabled);
    }

    public static ODocument getPermissionTag(String name) throws SqlInjectionException {
        PermissionTagDao dao = PermissionTagDao.getInstance();
        return dao.getByName(name);
    }
}
