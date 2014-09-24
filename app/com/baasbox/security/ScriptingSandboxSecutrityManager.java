package com.baasbox.security;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * Created by eto on 24/09/14.
 */
public class ScriptingSandboxSecutrityManager extends SecurityManager {

    private SecurityManager mDelegated;

    public static void init() {
        SecurityManager sm = System.getSecurityManager();
        System.setSecurityManager(new ScriptingSandboxSecutrityManager(sm));
    }

    private ScriptingSandboxSecutrityManager(SecurityManager sm) {
        mDelegated = sm;
    }

    @Override
    public Object getSecurityContext() {
        return mDelegated.getSecurityContext();
    }

    @Override
    public void checkPermission(Permission perm) {
        if (perm == null) {
            return;
        }
        if ("nashorn.JavaReflection".equals(perm.getName())) {
            throw new SecurityException("Invalid reflective access in scripting code");
//        } else if("baasbox.scripting".equals(perm.getName())){
//            super.checkPermission(perm);
        } else if (mDelegated != null) {
            mDelegated.checkPermission(perm);
        }
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if (mDelegated != null)
            mDelegated.checkPermission(perm, context);
    }

    @Override
    public void checkCreateClassLoader() {
        if (mDelegated != null)
            mDelegated.checkCreateClassLoader();
    }

    @Override
    public void checkAccess(Thread t) {
        if (mDelegated != null)
            mDelegated.checkAccess(t);
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        if (mDelegated != null)
            mDelegated.checkAccess(g);
    }

    @Override
    public void checkExit(int status) {
        if (mDelegated != null)
            mDelegated.checkExit(status);
    }

    @Override
    public void checkExec(String cmd) {
        if (mDelegated != null)
            mDelegated.checkExec(cmd);
    }

    @Override
    public void checkLink(String lib) {
        if (mDelegated != null)
            mDelegated.checkLink(lib);
    }

    @Override
    public void checkRead(FileDescriptor fd) {
        if (mDelegated != null)
            mDelegated.checkRead(fd);
    }

    @Override
    public void checkRead(String file) {
        if (mDelegated != null)
            mDelegated.checkRead(file);
    }

    @Override
    public void checkRead(String file, Object context) {
        if (mDelegated != null)
            mDelegated.checkRead(file, context);
    }

    @Override
    public void checkWrite(FileDescriptor fd) {
        if (mDelegated != null)
            mDelegated.checkWrite(fd);
    }

    @Override
    public void checkWrite(String file) {
        if (mDelegated != null)
            mDelegated.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
        if (mDelegated != null)
            mDelegated.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
        if (mDelegated != null)
            mDelegated.checkConnect(host, port);
    }

    @Override
    public void checkConnect(String host, int port, Object context) {
        if (mDelegated != null)
            mDelegated.checkConnect(host, port, context);
    }

    @Override
    public void checkListen(int port) {
        if (mDelegated != null)
            mDelegated.checkListen(port);
    }

    @Override
    public void checkAccept(String host, int port) {
        if (mDelegated != null)
            mDelegated.checkAccept(host, port);
    }

    @Override
    public void checkMulticast(InetAddress maddr) {
        if (mDelegated != null)
            mDelegated.checkMulticast(maddr);
    }

    @Override
    @Deprecated
    public void checkMulticast(InetAddress maddr, byte ttl) {
        if (mDelegated != null)
            mDelegated.checkMulticast(maddr, ttl);
    }

    @Override
    public void checkPropertiesAccess() {
        if (mDelegated != null)
            mDelegated.checkPropertiesAccess();
    }

    @Override
    public void checkPropertyAccess(String key) {
        if (mDelegated != null)
            mDelegated.checkPropertyAccess(key);
    }

    @Override
    public void checkPrintJobAccess() {
        if (mDelegated != null)
            mDelegated.checkPrintJobAccess();
    }


    @Override
    public void checkPackageAccess(String pkg) {
        if (mDelegated != null)
            mDelegated.checkPackageAccess(pkg);
    }

    @Override
    public void checkPackageDefinition(String pkg) {
        if (mDelegated != null)
            mDelegated.checkPackageDefinition(pkg);
    }

    @Override
    public void checkSetFactory() {
        if (mDelegated != null)
            mDelegated.checkSetFactory();
    }

    @Override
    public void checkSecurityAccess(String target) {
        if (mDelegated != null)
            mDelegated.checkSecurityAccess(target);
    }

    @Override
    public ThreadGroup getThreadGroup() {
        if (mDelegated != null)
            return mDelegated.getThreadGroup();
        return null;
    }
}
