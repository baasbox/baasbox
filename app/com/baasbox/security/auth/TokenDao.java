package com.baasbox.security.auth;

import com.baasbox.db.DbHelper;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Created by Andrea Tortorella on 5/14/15.
 */
public class TokenDao
{

    private static final String INDEX_NAME = "_BB_TokenStore";

    private final ODatabaseRecordTx db;

    private static final Charset UTF = Charset.forName("UTF-8");

    private static final ThreadLocal<ORecordBytes> RECORD_BYTES=
            new ThreadLocal<ORecordBytes>(){
                @Override
                protected ORecordBytes initialValue()
                {
                    return new ORecordBytes();
                }
            };

    public static TokenDao getInstance(){
        return new TokenDao();
    }

    TokenDao(){
        db = DbHelper.getConnection();
    }

    public void storeToken(String encodedToken,String forUser){
        OIndex<?> index = getIndex();
        ORecordBytes user = encode(forUser);
        index.put(encodedToken, user);
    }

    public boolean dropToken(String encodedToken){
        OIndex<?> index = getIndex();
        return index.remove(encodedToken);
    }

    public Optional<String> getUser(String encodedToken){
        OIndex<?> index = getIndex();
        Object rec =  index.get(encodedToken);
        if (rec instanceof ORecordBytes){
            return Optional.ofNullable(decode((ORecordBytes) rec));
        }
        return Optional.empty();
    }

    private OIndex<?> getIndex(){
        return db.getMetadata().getIndexManager().getIndex(INDEX_NAME);
    }

    private static String decode(ORecordBytes rec){
        byte[] bytes = rec.toStream();
        return new String(bytes,UTF);
    }

    private static ORecordBytes encode(String foruser){
        ORecordBytes bytes = RECORD_BYTES.get();
        bytes.reset(foruser.getBytes(UTF));
        return bytes;
    }


}
