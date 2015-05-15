package com.baasbox.security.auth;

import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.UUID;

/**
 * Created by Andrea Tortorella on 5/15/15.
 */
public class CredentialsDao
{
    private static final String MODEL_NAME = "_BB_DBCredentials";
    public static final String DB_USER = "dbuser";
    public static final String DB_PASSWORD = "dbpassword";
    public static final String SALT = "salt";
    public static final String PWD = "pwd";

    private final ODatabaseRecordTx db;

    private CredentialsDao(){
        db = DbHelper.getConnection();
    }

    public static CredentialsDao getInstance(){
        return new CredentialsDao();
    }

    public ODocument createUserCredentials(String username,String userPassword,String internalPassword){
        ODocument doc = new ODocument(MODEL_NAME);
        doc.field(DB_USER,username);
        doc.field(DB_PASSWORD, internalPassword);
        byte[] salt = Passwords.salt();
        byte[] hash = Passwords.hash(userPassword.toCharArray(), salt);
        doc.field(SALT,salt);
        doc.field(PWD, hash);
        doc.save();
        return doc;
    }

}
