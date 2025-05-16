package com.authlite.server;

import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public UserManager(String dbHost, int dbPort, String dbName, String dbUser, String dbPassword) {
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public boolean registerUser(String username, PublicKey publicKey) {
        String publicKeyEncoded = encodePublicKey(publicKey);

        try (DatabaseManager dbManager = new DatabaseManager(dbHost, dbPort, dbName, dbUser, dbPassword)) {
            boolean success = dbManager.addUser(username, publicKeyEncoded);
            if (success) {
                logger.info("Username '{}' registered successfully!", username);
            } else {
                logger.warn("Failed to register username '{}'. It might already exist.", username);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error during user registration for '{}'", username, e);
            return false;
        }
    }

    public PublicKey getUserPublicKey(String username) {
        try (DatabaseManager dbManager = new DatabaseManager(dbHost, dbPort, dbName, dbUser, dbPassword)) {
            String publicKeyEncoded = dbManager.getPublicKey(username);
            if (publicKeyEncoded == null) {
                logger.warn("Public key not found for username '{}'.", username);
                return null;
            }
            return decodePublicKey(publicKeyEncoded);
        } catch (Exception e) {
            logger.error("Error retrieving public key for '{}'", username, e);
            return null;
        }
    }

    public boolean userExists(String username) {
        try (DatabaseManager dbManager = new DatabaseManager(dbHost, dbPort, dbName, dbUser, dbPassword)) {
            return dbManager.getPublicKey(username) != null;
        } catch (Exception e) {
            logger.error("Error checking existence for username '{}'", username, e);
            return false;
        }
    }

    private String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    private PublicKey decodePublicKey(String publicKeyStr) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC"); 
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            logger.error("Failed to decode public key.", e);
            return null;
        }
    }
}