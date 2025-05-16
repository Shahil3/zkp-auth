package com.authlite.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChallengeManager {
    private final Map<String, ChallengeWithTimestamp> activeChallenges = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserManager userManager;
    private static final Logger logger = LoggerFactory.getLogger(ChallengeManager.class);

    public ChallengeManager(UserManager userManager){
        this.userManager = userManager;
    }

    public String generateChallenge(String username) {
        byte[] challengeBytes = new byte[32];
        random.nextBytes(challengeBytes);
        String challenge = Base64.getEncoder().encodeToString(challengeBytes);
        logger.info("challenge for {} created successfully! challenge is {}" , username , challenge);
        long timestamp = System.currentTimeMillis(); // Get current timestamp

        if(userManager.userExists(username)){
            activeChallenges.remove(username);
        }
        activeChallenges.put(username, new ChallengeWithTimestamp(challenge, timestamp)); // Store both

        return challenge; // Return only the challenge string
    }

    public boolean isChallengeValid(String username) {
        if(!userManager.userExists(username)){
            return false;
        }
        ChallengeWithTimestamp storedChallengeData = activeChallenges.get(username);
        if (storedChallengeData == null) {
            return false; // No challenge found for this user
        }

        //  Add a time limit (e.g., 60 seconds)
        long currentTime = System.currentTimeMillis();
        long challengeTime = storedChallengeData.timestamp;
        long timeDifference = currentTime - challengeTime;

        if (timeDifference > 60000) { // 60000 milliseconds = 60 seconds
            activeChallenges.remove(username); //remove invalid challenge
            return false; // Challenge has expired
        }
        
        return true;
    }

    public String getChallenge(String username) {
        ChallengeWithTimestamp challengeData = activeChallenges.get(username);
        if (challengeData != null) {
            return challengeData.challenge;
        }
        return null;
    }
    public void removeChallenge(String username){
        if(activeChallenges.containsKey(username)){
            activeChallenges.remove(username);
        }
    }

    // Inner class to store both challenge and timestamp
    private static class ChallengeWithTimestamp {
        final String challenge;
        final long timestamp;

        public ChallengeWithTimestamp(String challenge, long timestamp) {
            this.challenge = challenge;
            this.timestamp = timestamp;
        }
    }
}
