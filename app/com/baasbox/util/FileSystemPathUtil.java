package com.baasbox.util;

//adapted from http://svn.apache.org/repos/asf/jackrabbit/branches/2.6/jackrabbit-core/src/main/java/org/apache/jackrabbit/core/fs/FileSystemPathUtil.java


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;

/**
 * Utility class for handling paths in a file system.
 */
public final class FileSystemPathUtil {

    /**
     * Array of lowercase hexadecimal characters used in creating hex escapes.
     */
    private static final char[] HEX_TABLE = "0123456789abcdef".toCharArray();

    /**
     * The escape character used to mark hex escape sequences.
     */
    private static final char ESCAPE_CHAR = '_';

    /**
     * The list of characters that are not encoded by the <code>escapeName(String)</code>
     * and <code>unescape(String)</code> methods. They contains the characters
     * which can safely be used in file names:
     */
    public static final BitSet SAFE_NAMECHARS;

    /**
     * The list of characters that are not encoded by the <code>escapePath(String)</code>
     * and <code>unescape(String)</code> methods. They contains the characters
     * which can safely be used in file paths:
     */
    public static final BitSet SAFE_PATHCHARS;

    static {
        // build list of valid name characters
        SAFE_NAMECHARS = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            SAFE_NAMECHARS.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            SAFE_NAMECHARS.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            SAFE_NAMECHARS.set(i);
        }
        SAFE_NAMECHARS.set('-');
        SAFE_NAMECHARS.set('_');
        SAFE_NAMECHARS.set('.');

        // build list of valid path characters (includes name characters)
        SAFE_PATHCHARS = (BitSet) SAFE_NAMECHARS.clone();
        SAFE_PATHCHARS.set(File.separatorChar);
    }

    /**
     * private constructor
     */
    private FileSystemPathUtil() {
    }

    /**
     * Escapes the given string using URL encoding for all bytes not included
     * in the given set of safe characters.
     *
     * @param s the string to escape
     * @param safeChars set of safe characters (bytes)
     * @return escaped string
     */
    private static String escape(String s, BitSet safeChars) {
        byte[] bytes = s.getBytes();
        StringBuilder out = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xff;
            if (safeChars.get(c) && c != ESCAPE_CHAR) {
                out.append((char) c);
            } else {
                out.append(ESCAPE_CHAR);
            }
        }
        return out.toString();
    }

    /**
     * Encodes the specified <code>path</code>. Same as
     * <code>{@link #escapeName(String)}</code> except that the separator
     * character <b><code>/</code></b> is regarded as a legal path character
     * that needs no escaping.
     *
     * @param path the path to encode.
     * @return the escaped path
     */
    public static String escapePath(String path) {
        return escape(path, SAFE_PATHCHARS);
    }

    /**
     * Encodes the specified <code>name</code>. Same as
     * <code>{@link #escapePath(String)}</code> except that the separator character
     * <b><code>/</code></b> is regarded as an illegal character that needs
     * escaping.
     *
     * @param name the name to encode.
     * @return the escaped name
     */
    public static String escapeName(String name) {
        return escape(name, SAFE_NAMECHARS);
    }

    /**
     * Decodes the specified path/name.
     *
     * @param pathOrName the escaped path/name
     * @return the unescaped path/name
     */
    public static String unescape(String pathOrName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pathOrName.length());
        for (int i = 0; i < pathOrName.length(); i++) {
            char c = pathOrName.charAt(i);
            if (c == ESCAPE_CHAR) {
                try {
                    out.write(Integer.parseInt(pathOrName.substring(i + 1, i + 3), 16));
                } catch (NumberFormatException e) {
                    IllegalArgumentException iae = new IllegalArgumentException("Failed to unescape escape sequence");
                    iae.initCause(e);
                    throw iae;
                }
                i += 2;
            } else {
                out.write(c);
            }
        }
        return new String(out.toByteArray());
    }

    /**
     * Tests whether the specified path represents the root path, i.e. "/".
     *
     * @param path path to test
     * @return true if the specified path represents the root path; false otherwise.
     */
    public static boolean denotesRoot(String path) {
        return path.equals(File.separator);
    }

    /**
     * Checks if <code>path</code> is a valid path.
     *
     * @param path the path to be checked
     * @throws FileSystemException If <code>path</code> is not a valid path
     */
    public static void checkFormat(String path) throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        // path must be absolute, i.e. starting with '/'
        if (!path.startsWith(File.separator)) {
            throw new IllegalArgumentException("not an absolute path: " + path);
        }

        // trailing '/' is not allowed (except for root path)
        if (path.endsWith(File.separator) && path.length() > 1) {
            throw new IllegalArgumentException("malformed path: " + path);
        }

        String[] names = path.split(File.separator);
        for (int i = 1; i < names.length; i++) {
            // name must not be empty
            if (names[i].length() == 0) {
                throw new IllegalArgumentException("empty name: " + path);
            }
            // leading/trailing whitespace is not allowed
            String trimmed = names[i].trim();
            if (!trimmed.equals(names[i])) {
                throw new IllegalArgumentException("illegal leading or trailing whitespace in name: " + path);
            }
        }
    }

    /**
     * Returns the parent directory of the specified <code>path</code>.
     *
     * @param path a file system path denoting a directory or a file.
     * @return the parent directory.
     */
    public static String getParentDir(String path) {
        int pos = path.lastIndexOf(File.separator);
        if (pos > 0) {
            return path.substring(0, pos);
        }
        return File.separator;
    }

    /**
     * Returns the name of the specified <code>path</code>.
     *
     * @param path a file system path denoting a directory or a file.
     * @return the name.
     */
    public static String getName(String path) {
        int pos = path.lastIndexOf(File.separator);
        if (pos != -1) {
            return path.substring(pos + 1);
        }
        return path;
    }

}