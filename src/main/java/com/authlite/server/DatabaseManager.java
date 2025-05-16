package com.authlite.server;

import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager implements AutoCloseable{
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private final String url;
    private final String user;
    private final String password;

    private Connection conn;

    public DatabaseManager(String host, int port, String dbName, String user, String password) {
        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        this.user = user;
        this.password = password;
        connect();
    }

    private void connect() {
        try {
            conn = DriverManager.getConnection(url, user, password);
            logger.info("Connected to database: {}", url);
            createTables();
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", url, e);
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id SERIAL PRIMARY KEY, " +
                     "username VARCHAR(100) UNIQUE NOT NULL, " +
                     "public_key TEXT NOT NULL, " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            logger.info("Users table verified/created successfully.");
        } catch (SQLException e) {
            logger.error("Failed to create tables.", e);
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    public boolean addUser(String username, String publicKey) {
        if (conn == null) {
            logger.error("Database connection is not initialized.");
            return false;
        }
        String sql = "INSERT INTO users (username, public_key) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, publicKey);
            pstmt.executeUpdate();
            logger.info("New user added: {}", username);
            return true;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // Unique constraint violation
                logger.warn("Attempt to add duplicate username: {}", username);
            } else {
                logger.error("Failed to add user: {}", username, e);
            }
            return false;
        }
    }

    public String getPublicKey(String username) {
        if (conn == null) {
            logger.error("Database connection is not initialized.");
            return null;
        }
        String sql = "SELECT public_key FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                logger.info("Public key retrieved for username: {}", username);
                return rs.getString("public_key");
            } else {
                logger.warn("Username not found: {}", username);
                return null;
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve public key for username: {}", username, e);
            return null;
        }
    }

    public void close() {
        try {
            if (conn != null) {
                conn.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.warn("Error closing database connection.", e);
        }
    }
}