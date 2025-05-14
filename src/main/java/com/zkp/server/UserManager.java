package com.zkp.server;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager {
    private final Map<String , PublicKey>userDatabase = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    public void registerUser(String username , PublicKey publicKey){
        userDatabase.put(username , publicKey);
        logger.info("username : " + username + " registered!");
    }

    public PublicKey getUserPublicKey(String username){
        return userDatabase.get(username);
    }

    public boolean userExists(String username){
        return userDatabase.containsKey(username);
    }
}
