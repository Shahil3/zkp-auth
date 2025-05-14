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
        // Simulate client sending JSON to server
        Map<String, String> requestMap = objectMapper.readValue(jsonPayload, Map.class);
        Map<String, String> responseMap = authServer.handleRequest(requestMap);
        return objectMapper.writeValueAsString(responseMap);
    }
}