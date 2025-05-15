package com.zkp;

import com.zkp.client.*;
import com.zkp.server.*;

public class TestAuthFlow {

    public static void main(String[] args) {
        try {
            // 1. Define Database connection parameters
            String dbHost = "localhost";
            int dbPort = 5432;
            String dbName = "zkpdb";
            String dbUser = "shahil";
            String dbPassword = "Shahil#03";

            // 2. Setup Server Side Components
            UserManager userManager = new UserManager(dbHost, dbPort, dbName, dbUser, dbPassword);
            ChallengeManager challengeManager = new ChallengeManager(userManager);
            AuthServer authServer = new AuthServer(userManager, challengeManager);

            // 3. Setup Client Side Components
            KeyManager keyManager = new KeyManager();
            NetworkClient fakeNetworkClient = new FakeNetworkClient(authServer); // <-- special fake network

            AuthClient authClient = new AuthClient(keyManager, fakeNetworkClient, "http://fake-server");

            // 4. Client: Signup
            boolean signupSuccess = authClient.signup("john_doe");
            if (signupSuccess) {
                System.out.println("Signup successful!");

                // 5. Client: Login
                boolean loginSuccess = authClient.logIn("john_doe");
                if (loginSuccess) {
                    System.out.println("Login successful!");
                } else {
                    System.out.println("Login failed!");
                }
            } else {
                System.out.println("Signup failed!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}