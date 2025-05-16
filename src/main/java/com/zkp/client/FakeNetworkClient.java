package com.zkp.client;

import com.zkp.server.AuthServer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class FakeNetworkClient extends NetworkClient {

    private final AuthServer authServer;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public FakeNetworkClient(AuthServer authServer) {
        this.authServer = authServer;
    }

    @Override
    public String post(String url, String jsonPayload) throws Exception {
        Map<String, String> requestMap = objectMapper.readValue(jsonPayload, Map.class);
        return simulateRequest(requestMap);
    }

    @Override
    public String postWithAuth(String url, String jsonPayload, String bearerToken) throws Exception {
        Map<String, String> requestMap = objectMapper.readValue(jsonPayload, Map.class);
        if (bearerToken != null && !bearerToken.isEmpty()) {
            requestMap.put("token", bearerToken);  // Simulate token in header
        }
        return simulateRequest(requestMap);
    }

    @Override
    public String getWithAuth(String url, String bearerToken) throws Exception {
        Map<String, String> requestMap = Map.of(
                "type", "getprofile",
                "token", bearerToken
        );
        return simulateRequest(requestMap);
    }

    private String simulateRequest(Map<String, String> requestMap) throws Exception {
        Map<String, String> responseMap = authServer.handleRequest(requestMap);
        return objectMapper.writeValueAsString(responseMap);
    }
}