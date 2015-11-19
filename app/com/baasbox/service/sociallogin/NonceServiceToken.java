/*
 * Copyright (c) 2015.
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
 * 
 */

package com.baasbox.service.sociallogin;

import com.baasbox.configuration.Application;

import java.io.Reader;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.util.Base64;
import java.util.Date;
import org.bouncycastle.openssl.PEMReader;

public class NonceServiceToken {
	
	private static final String DEFAULT_LAYER_PRIVATE_KEY = "";
	private static final String DEFAULT_LAYER_PROVIDER_ID = "";
	private static final String DEFAULT_LAYER_KEY_ID = "";

    public static String getHexString(byte[] b) throws Exception {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result +=
                Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }

    private KeyPair GetKeyPairFromPKCSKey(String key) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Reader fRd = new StringReader(key);
        try {
        	PEMReader pemReader = new PEMReader(fRd);
        	KeyPair pair = (KeyPair)pemReader.readObject();
        	pemReader.close();
        	return pair;
        } catch (Exception ex) {
        	throw ex;
        }
    }
    
    private String GetSHA256 (String text, PrivateKey privateKey) throws Exception {
            Signature signature = Signature.getInstance("SHA256withRSA","BC");
            signature.initSign(privateKey);
            signature.update(text.getBytes());      
            byte[] data = signature.sign();
            String hexString = getHexString(data);
            return hexString;
    }
    
	public String GetNonceToken(String username, String nonce) throws Exception {
		String token = "";

		/* LOAD LAYER PARAMETERS */
		String layerProviderID = Application.LAYER_API_PROVIDER_ID.getValueAsString();
	    String layerKeyID = Application.LAYER_API_KEY_ID.getValueAsString();
	    String layerPrivateKey = Application.LAYER_API_PRIVATE_KEY.getValueAsString();

    	layerProviderID = (layerProviderID == null) ? DEFAULT_LAYER_PROVIDER_ID : layerProviderID;
    	layerKeyID = (layerKeyID == null) ? DEFAULT_LAYER_KEY_ID : layerKeyID;
    	layerPrivateKey = (layerPrivateKey == null) ? DEFAULT_LAYER_PRIVATE_KEY : layerPrivateKey;
    
    	// Read key from PEM (---- BEGIN RSA .... key format)
    	KeyPair pair = GetKeyPairFromPKCSKey(layerPrivateKey);
    	
    	/* CREATE HEADER AND SHA256*/
    	String header = "{\"typ\":\"JWT\",\"alg\":\"RS256\",\"cty\":\"layer-eit;v=1\",\"kid\":\"" + layerKeyID + "\"}";
    	String h = Base64.getEncoder().encodeToString(header.getBytes());

    	/* CREATE CLAIM AND SH 256 */
    	long currentTimeInSeconds = Math.round(new Date().getTime() / 1000);
    	long expirationTime = currentTimeInSeconds + 10000;
    	
    	// DEBUG ONLY    	
    	String claim = "{\"iss\":\"" + layerProviderID + "\",\"prn\":\"" + username + "\",\"iat\":" + currentTimeInSeconds + ",\"exp\":" + expirationTime + ",\"nce\":\"" + nonce + "\"}";
    	String c = Base64.getEncoder().encodeToString(claim.getBytes());
    	    	
    	/* CREATE JWT FOR LAYER*/
    	String n = h + "." + c;
    	String nSha = GetSHA256(n, pair.getPrivate());
    	token = n + "." + nSha;
    	
		return Base64.getEncoder().encodeToString(token.getBytes());
		    
	}
}

