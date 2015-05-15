package com.baasbox.security.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Created by Andrea Tortorella on 5/15/15.
 */
public class Passwords
{
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    private Passwords(){}

    public static byte[] salt(){
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password,byte[] salt){
        PBEKeySpec spec = new PBEKeySpec(password,salt,ITERATIONS,KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            throw new AssertionError("Error hashing the password",e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean isExpectedPassword(char[] password,byte[] salt,byte[] expected){
        byte[] pwdHash = hash(password,salt);
        Arrays.fill(password,Character.MIN_VALUE);
        if (pwdHash.length != expected.length) return false;
        for (int i = 0; i < pwdHash.length; i++) {
            if (pwdHash[i] != expected[i]) return false;
        }
        return true;
    }

}
