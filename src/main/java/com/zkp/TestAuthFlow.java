package com.zkp;

import com.zkp.client.*;
import com.zkp.server.*;

import java.util.Map;

public class TestAuthFlow {

    public static void main(String[] args) {
        try {
            // 1. Setup Server Side Components
            UserManager userManager = new UserManager();
            ChallengeManager challengeManager = new ChallengeManager(userManager);
            AuthServer authServer = new AuthServer(userManager, challengeManager);

            // 2. Setup Client Side Components
            KeyManager keyManager = new KeyManager();
            NetworkClient fakeNetworkClient = new FakeNetworkClient(authServer); // <-- special fake network

            AuthClient authClient = new AuthClient(keyManager, fakeNetworkClient, "http://fake-server");

            // 3. Client: Signup
            boolean signupSuccess = authClient.signup("john_doe");
            if (signupSuccess) {
                System.out.println("Signup successful!");

                // 4. Client: Login
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