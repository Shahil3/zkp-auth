package com.authlite.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthClient {

    private final KeyManager keyManager;
    private final NetworkClient networkClient;
    private final String urlString;
    private String sessionToken;

    private static final Logger logger = LoggerFactory.getLogger(AuthClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AuthClient(KeyManager keyManager, NetworkClient networkClient, String urlString) {
        this.keyManager = keyManager;
        this.networkClient = networkClient;
        this.urlString = urlString;
        logger.info("AuthClient Initialized!");
    }

    public boolean signup(String username) throws Exception {
        Map<String, String> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("type", "signup");
        userMap.put("publicKey", Base64.getEncoder().encodeToString(this.keyManager.getPublicKey().getEncoded()));

        String payLoad = objectMapper.writeValueAsString(userMap);
        logger.info("Sending signup request to server for username: {}", username);
        Map<String, String> response = objectMapper.readValue(networkClient.post(this.urlString, payLoad), Map.class);

        if (!"ok".equalsIgnoreCase(response.get("status"))) {
            logger.error("Signup failed: {}", response.get("message"));
            return false;
        }

        logger.info("Signup successful: {}", response.get("message"));
        return true;
    }

    public boolean logIn(String username) throws Exception {
        Map<String, String> challengeRequest = new HashMap<>();
        challengeRequest.put("username", username);
        challengeRequest.put("type", "challenge");

        String challengePayload = objectMapper.writeValueAsString(challengeRequest);
        String challengeResponseString = networkClient.post(urlString, challengePayload);
        Map<String, String> challengeResponse = objectMapper.readValue(challengeResponseString, Map.class);

        if (!"ok".equalsIgnoreCase(challengeResponse.get("status"))) {
            logger.error("Challenge request failed: {}", challengeResponse.get("message"));
            return false;
        }

        String challenge = challengeResponse.get("challenge");
        String signedChallenge = CryptoUtils.sign(keyManager.getPrivateKey(), challenge);

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("type", "login");
        loginRequest.put("signedChallenge", signedChallenge);

        String loginPayload = objectMapper.writeValueAsString(loginRequest);
        String loginResponseString = networkClient.post(urlString, loginPayload);
        Map<String, String> loginResponse = objectMapper.readValue(loginResponseString, Map.class);

        if (!"ok".equalsIgnoreCase(loginResponse.get("status"))) {
            logger.error("Login failed: {}", loginResponse.get("message"));
            return false;
        }

        this.sessionToken = loginResponse.get("token");
        logger.info("Login successful. Token received.");
        return true;
    }

    public boolean getProfile() throws Exception {
        Map<String, String> profileRequest = new HashMap<>();
        profileRequest.put("type", "getprofile");
        profileRequest.put("token", sessionToken);

        String profilePayload = objectMapper.writeValueAsString(profileRequest);
        String profileResponseString = networkClient.post(urlString, profilePayload);
        Map<String, String> profileResponse = objectMapper.readValue(profileResponseString, Map.class);

        if (!"ok".equalsIgnoreCase(profileResponse.get("status"))) {
            logger.error("Profile request failed: {}", profileResponse.get("message"));
            return false;
        }

        logger.info("User profile retrieved: username = {}", profileResponse.get("username"));
        return true;
    }

    public String getSessionToken() {
        return sessionToken;
    }
} 
