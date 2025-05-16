package com.zkp.server;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.security.PublicKey;
import java.security.Signature;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;

public class AuthServer {

    private static final Logger logger = LoggerFactory.getLogger(AuthServer.class);
    private final UserManager userManager;
    private final ChallengeManager challengeManager;
    private final Key jwtSecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION_MILLIS = 30 * 60 * 1000; // 30 minutes

    public AuthServer(UserManager userManager, ChallengeManager challengeManager) {
        this.userManager = userManager;
        this.challengeManager = challengeManager;
    }

    public Map<String, String> handleRequest(Map<String, String> request) {
        String type = request.get("type");

        switch (type.toLowerCase()) {
            case "signup": return handleSignup(request);
            case "challenge": return handleChallengeRequest(request);
            case "login": return handleLoginVerification(request);
            case "logout": return handleLogout(request);
            case "getprofile": return handleProtectedProfile(request);
            default:
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
                String token = generateJwtToken(username);
                return Map.of("status", "ok", "message", "Login successful", "token", token);
            } else {
                logger.warn("Invalid signature during login attempt for {}", username);
                return Map.of("status", "fail", "message", "Invalid signature");
            }
        } catch (Exception e) {
            logger.error("Error during login verification", e);
            return Map.of("status", "fail", "message", "Login error: " + e.getMessage());
        }
    }

    private Map<String, String> handleLogout(Map<String, String> request) {
        String token = request.get("token");
        if (!isTokenValid(token)) {
            return Map.of("status", "fail", "message", "Invalid or expired token");
        }
        String username = getUsernameFromToken(token);
        logger.info("User {} logged out (client-side only)", username);
        return Map.of("status", "ok", "message", "Logged out successfully");
    }

    private Map<String, String> handleProtectedProfile(Map<String, String> request) {
        String token = request.get("token");
        if (!isTokenValid(token)) {
            return Map.of("status", "fail", "message", "Unauthorized");
        }
        String username = getUsernameFromToken(token);
        logger.info("Profile access granted to {}", username);
        return Map.of("status", "ok", "username", username);
    }

    private String generateJwtToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MILLIS))
                .signWith(jwtSecretKey)
                .compact();
    }

    private boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(jwtSecretKey).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
