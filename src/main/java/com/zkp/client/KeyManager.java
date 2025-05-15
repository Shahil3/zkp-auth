package com.zkp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;  // <-- Added
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;  // <-- Added
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec; // <-- Added
import java.security.spec.X509EncodedKeySpec; // <-- Added
import java.security.MessageDigest;
import java.util.Base64;

public class KeyManager {

    private static final Logger logger = LoggerFactory.getLogger(KeyManager.class);

    private KeyPair keyPair;
    private byte[] salt;
    private byte[] iv;

    public KeyManager() throws Exception {
        this.keyPair = generateKeyPair();
        this.salt = new byte[16];
        this.iv = new byte[16];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
    }

    public KeyPair generateKeyPair() throws Exception {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256);
            KeyPair pair = keyGen.generateKeyPair();
            logger.info("Successfully generated keypair.");
            return pair;
        } catch (Exception e) {
            logger.error("Failed to generate Key Pair", e);
            throw new RuntimeException("KeyPair generation failed", e);
        }
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public void storeKeyPair(String username , String password) throws Exception {
        // 1. Derive AES key from password
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

        // 2. Encrypt private key
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
        byte[] encryptedPrivateKey = cipher.doFinal(this.keyPair.getPrivate().getEncoded());

        // 3. Define paths
        Path directory = Paths.get(System.getProperty("user.home"), ".zkp-auth", username);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path privateKeyPath = directory.resolve("private.key");
        Path publicKeyPath = directory.resolve("public.key");
        Path metadataPath = directory.resolve("key_metadata");

        // 4. Save encrypted private key
        try (FileOutputStream privateOut = new FileOutputStream(privateKeyPath.toFile())) {
            privateOut.write(encryptedPrivateKey);
        }

        // 5. Save public key (plain)
        try (FileOutputStream publicOut = new FileOutputStream(publicKeyPath.toFile())) {
            publicOut.write(this.keyPair.getPublic().getEncoded());
        }

        // 6. Save salt and IV needed for decryption
        try (FileOutputStream metaOut = new FileOutputStream(metadataPath.toFile())) {
            metaOut.write(salt);
            metaOut.write(iv);
        }

        logger.info("Keys saved successfully in ~/.zkp-auth/{}/", username);
    }

    public KeyPair loadKeyPair(String username, String password) throws Exception {
        Path directory = Paths.get(System.getProperty("user.home"), ".zkp-auth", username);
        Path privateKeyPath = directory.resolve("private.key");
        Path publicKeyPath = directory.resolve("public.key");
        Path metadataPath = directory.resolve("key_metadata");
    
        if (!Files.exists(privateKeyPath) || !Files.exists(publicKeyPath) || !Files.exists(metadataPath)) {
            logger.error("Key files not found. Have you generated keys yet?");
            throw new IOException("Missing key files in ~/.zkp-auth/. Cannot load keys.");
        }
    
        try {
            // 1. Read salt and iv
            byte[] metaBytes = Files.readAllBytes(metadataPath);
            System.arraycopy(metaBytes, 0, this.salt, 0, 16);
            System.arraycopy(metaBytes, 16, this.iv, 0, 16);
    
            // 2. Derive AES key from password
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), this.salt, 65536, 256);
            SecretKeySpec secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    
            // 3. Decrypt private key
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(this.iv));
            byte[] encryptedPrivateKey = Files.readAllBytes(privateKeyPath);
            byte[] decryptedPrivateKeyBytes = cipher.doFinal(encryptedPrivateKey);
    
            // 4. Read public key bytes
            byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
    
            // 5. Rebuild PrivateKey and PublicKey objects
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decryptedPrivateKeyBytes));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    
            this.keyPair = new KeyPair(publicKey, privateKey);
            logger.info("Successfully loaded KeyPair from ~/.zkp-auth/");
            return this.keyPair;
    
        } catch (Exception e) {
            logger.error("Failed to load KeyPair — Possible wrong password or corrupted files", e);
            throw new RuntimeException("Failed to load keys: " + e.getMessage(), e);
        }
    }

    public String getPublicKeyFingerprint() throws Exception {
        if (keyPair == null || keyPair.getPublic() == null) {
            throw new IllegalStateException("Public key is not initialized.");
        }
    
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        byte[] fingerprintBytes = digest.digest(publicKeyBytes);
    
        return Base64.getEncoder().encodeToString(fingerprintBytes);
    }
}