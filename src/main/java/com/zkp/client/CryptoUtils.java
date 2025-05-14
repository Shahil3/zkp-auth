package com.zkp.client;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class CryptoUtils {

    public static String sign(PrivateKey privateKey, String data) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(data.getBytes("UTF-8"));
        byte[] signature = signer.sign();
        return Base64.getEncoder().encodeToString(signature);
    }
}