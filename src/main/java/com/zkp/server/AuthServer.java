package com.zkp.server;

import java.security.PublicKey;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthServer {

    private static final Logger logger = LoggerFactory.getLogger(AuthServer.class);

    private final UserManager userManager;
    private final ChallengeManager challengeManager;

    public AuthServer(UserManager userManager, ChallengeManager challengeManager) {
        this.userManager = userManager;
        this.challengeManager = challengeManager;
    }

    public Map<String, String> handleRequest(Map<String, String> request) {
        String type = request.get("type");

        if ("signup".equalsIgnoreCase(type)) {
            return handleSignup(request);
        } else if ("challenge".equalsIgnoreCase(type)) {
            return handleChallengeRequest(request);
        } else if ("login".equalsIgnoreCase(type)) {
            return handleLoginVerification(request);
        } else {
            logger.error("Unknown request type: {}", type);
            return Map.of("status", "fail", "message", "Unknown request type");
        }
    }

    private Map<String, String> handleSignup(Map<String, String> request) {
        try {
            String username = request.get("username");
            String publicKeyBase64 = request.get("publicKey");

            if (userManager.userExists(username)) {
                logger.warn("Signup attempt with already registered username: {}", username);
                return Map.of("status", "fail", "message", "Username already registered");
            }

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            userManager.registerUser(username, publicKey);
            logger.info("User {} registered successfully", username);

            return Map.of("status", "ok", "message", "Signup successful");
        } catch (Exception e) {
            logger.error("Error during signup", e);
            return Map.of("status", "fail", "message", "Signup error: " + e.getMessage());
        }
    }

    private Map<String, String> handleChallengeRequest(Map<String, String> request) {
        try {
            String username = request.get("username");

            if (!userManager.userExists(username)) {
                logger.warn("Challenge requested for unknown username: {}", username);
                return Map.of("status", "fail", "message", "Unknown username");
            }

            String challenge = challengeManager.generateChallenge(username);
            logger.info("Challenge generated for {}", username);

            return Map.of("status", "ok", "challenge", challenge);
        } catch (Exception e) {
            logger.error("Error generating challenge", e);
            return Map.of("status", "fail", "message", "Challenge error: " + e.getMessage());
        }
    }

    private Map<String, String> handleLoginVerification(Map<String, String> request) {
        try {
            String username = request.get("username");
            String signedChallengeBase64 = request.get("signedChallenge");

            if (!challengeManager.isChallengeValid(username)) {
                logger.warn("Invalid or expired challenge for {}", username);
                return Map.of("status", "fail", "message", "Invalid or expired challenge");
            }

            String originalChallenge = challengeManager.getChallenge(username);
            challengeManager.removeChallenge(username);

            PublicKey publicKey = userManager.getUserPublicKey(username);
            if (publicKey == null) {
                logger.error("No public key found for {}", username);
                return Map.of("status", "fail", "message", "Public key not found");
            }

            byte[] signedChallengeBytes = Base64.getDecoder().decode(signedChallengeBase64);

            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(originalChallenge.getBytes("UTF-8"));

            if (verifier.verify(signedChallengeBytes)) {
                logger.info("Login successful for {}", username);
                return Map.of("status", "ok", "message", "Login successful");
            } else {
                logger.warn("Invalid signature during login attempt for {}", username);
                return Map.of("status", "fail", "message", "Invalid signature");
            }
        } catch (Exception e) {
            logger.error("Error during login verification", e);
            return Map.of("status", "fail", "message", "Login error: " + e.getMessage());
        }
    }
}