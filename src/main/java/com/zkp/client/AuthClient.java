package com.zkp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthClient {

    private final KeyManager keyManager;
    private final NetworkClient networkClient;
    private final String urlString;
    private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AuthClient(KeyManager keyManager, NetworkClient networkClient, String urlString) {
        this.keyManager = keyManager;
        this.networkClient = networkClient;
        this.urlString = urlString;
        logger.info("AuthClient Initiallized!");
    }

    public boolean signup(String username) throws Exception {

        Map<String , String> userMap =  new HashMap<>();
        userMap.put("username" , username);
        userMap.put("type" , "signup");
        userMap.put("publicKey" , Base64.getEncoder().encodeToString(this.keyManager.getPublicKey().getEncoded()));
        
        String payLoad = objectMapper.writeValueAsString(userMap);
        logger.info("Sending signUp request to server from client username : " + username);
        Map<String , String> response = objectMapper.readValue(networkClient.post(this.urlString , payLoad) , Map.class);

        

        if(!"ok".equalsIgnoreCase(response.get("status"))){
            logger.error("Response recieved -> Status : " + response.get("status") + "\n" + "message : " + response.get("message"));
            return false;
        }
        
        logger.info("Response recieved -> Status : " + response.get("status") + "\n" + "message : " + response.get("message"));
        // logIn(username);
        return true;
    }

    public boolean logIn(String username) throws Exception {
        // 1. Ask server for challenge
        Map<String, String> challengeRequest = new HashMap<>();
        challengeRequest.put("username", username);
        challengeRequest.put("type", "challenge");
        
        String challengePayload = objectMapper.writeValueAsString(challengeRequest);
        String challengeResponseString = networkClient.post(urlString, challengePayload);
        Map<String, String> challengeResponse = objectMapper.readValue(challengeResponseString, Map.class);
    
        if (!"ok".equalsIgnoreCase(challengeResponse.get("status"))) {
            logger.error("Challenge request failed: " + challengeResponse.get("message"));
            return false;
        }
    
        String challenge = challengeResponse.get("challenge");
    
        // 2. Sign the challenge
        String signedChallenge = CryptoUtils.sign(keyManager.getPrivateKey(), challenge);
    
        // 3. Send signed challenge back
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("type", "login");
        loginRequest.put("signedChallenge", signedChallenge);
    
        String loginPayload = objectMapper.writeValueAsString(loginRequest);
        String loginResponseString = networkClient.post(urlString, loginPayload);
        Map<String, String> loginResponse = objectMapper.readValue(loginResponseString, Map.class);
    
        if (!"ok".equalsIgnoreCase(loginResponse.get("status"))) {
            logger.error("Login failed: " + loginResponse.get("message"));
            return false;
        }
    
        logger.info("Login successful: " + loginResponse.get("message"));
        return true;
    }
}
