package com.baasbox.security.auth;

import ch.qos.logback.classic.db.DBHelper;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.db.DbHelper;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
        doc.field(SALT, Base64.getEncoder().encodeToString(salt));
        doc.field(PWD, Base64.getEncoder().encodeToString(hash));
        doc.save();
        return doc;
    }

    public Optional<String> getCredentials(String username) throws SqlInjectionException{
        QueryParams criteria = QueryParams.getInstance().where(DB_USER+"=?").params(new String[]{username});
        OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, false, criteria);
        List<ODocument> res = DbHelper.selectCommandExecute(command,criteria.getParams());
        if (res.size()!=1){
            return Optional.empty();
        }
        ODocument stored = res.get(0);
        return Optional.of(stored.field(DB_PASSWORD));
    }


    public Optional<String> getCredentials(String username,String password) throws SqlInjectionException
    {
        QueryParams criteria = QueryParams.getInstance().where(DB_USER+"=?").params(new String[]{username});
        OCommandRequest command = DbHelper.selectCommandBuilder(MODEL_NAME, false, criteria);
        List<ODocument> res = DbHelper.selectCommandExecute(command, criteria.getParams());
        if (res.size()!=1){
            return Optional.empty();
        }
        ODocument storedCredentials = res.get(0);
        byte[] pwdHashed = Base64.getDecoder().decode(storedCredentials.<String>field(PWD));
        byte[] salt = Base64.getDecoder().decode(storedCredentials.<String>field(SALT));
        if(Passwords.isExpectedPassword(password.toCharArray(),salt,pwdHashed)){
            return Optional.of(storedCredentials.field(DB_PASSWORD));
        }
        return Optional.empty();
    }
}
