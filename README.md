# zkp-auth

zkp-auth is a lightweight, modular authentication system based on **Zero Knowledge Proof** principles, providing secure, passwordless user authentication using public key cryptography.

It is designed for developers who require secure, simple, and easily integratable authentication mechanisms in Java-based applications.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Technology Stack](#technology-stack)
- [How It Works](#how-it-works)
  - [Signup Flow](#signup-flow)
  - [Login Flow](#login-flow)
- [Installation](#installation)
- [Usage Example](#usage-example)
- [Future Improvements](#future-improvements)
- [License](#license)

## Overview

zkp-auth allows authentication without passwords.  
It leverages public-private key cryptography where users prove their identity without ever transmitting their private keys.  
The system uses a simple challenge-response mechanism for login, ensuring secure user verification based on Zero Knowledge Proof principles.

## Architecture

**Client Side**
- Key generation and secure private key storage
- Public key sharing at signup
- Challenge signing during login
- Secure communication (abstracted via HTTP client)

**Server Side**
- Public key registration and secure storage
- Challenge generation and lifecycle management
- Signature verification against stored public keys

## Project Structure

```
/zkp-auth/
├── client/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com/zkp/client/
│   │   │   │   │   ├── KeyManager.java
│   │   │   │   │   ├── AuthClient.java
│   │   │   │   │   ├── CryptoUtils.java
│   │   │   │   │   ├── NetworkClient.java
│   │   │   │   │   └── Exceptions.java (optional)
│   │   └── test/
│   │       └── java/com/zkp/client/
├── server/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com/zkp/server/
│   │   │   │   │   ├── AuthServer.java
│   │   │   │   │   ├── UserManager.java
│   │   │   │   │   ├── ChallengeManager.java
│   │   │   │   │   ├── CryptoUtils.java
│   │   │   │   │   └── Exceptions.java (optional)
│   │   └── test/
│   │       └── java/com/zkp/server/
├── README.md
├── LICENSE
└── build.gradle (or pom.xml if Maven)
```

## Technology Stack

- Java 11 or higher
- ECDSA (Elliptic Curve Digital Signature Algorithm)
- PBKDF2WithHmacSHA256 for password-based key encryption
- AES for private key storage
- SecureRandom for challenge generation
- Jackson for JSON serialization/deserialization
- SLF4J for logging
- Maven for project management

## How It Works

### Signup Flow

1. **Key Generation**:  
   The client generates an ECDSA keypair (private key + public key).

2. **Signup Request**:  
   The client sends the username and public key (Base64 encoded) to the server.

3. **Server Registration**:  
   The server stores the public key associated with the username.

4. **Confirmation**:  
   The server responds confirming successful registration.

### Login Flow

1. **Challenge Request**:  
   The client sends a login request with the username to the server.

2. **Challenge Issuance**:  
   The server generates a random challenge and sends it to the client.

3. **Challenge Signing**:  
   The client signs the challenge using its private key.

4. **Signed Challenge Submission**:  
   The client sends back the signed challenge.

5. **Verification**:  
   The server verifies the signature using the stored public key.

6. **Authentication Result**:  
   Based on the verification, the server accepts or rejects the login.

## Installation

1. Clone the repository:

```bash
git clone https://github.com/yourusername/zkp-auth.git
```

2. Build using Maven:

```bash
cd zkp-auth
mvn clean install
```

3. Import the modules into your Java project.

## TODO

- [ ] Integrate a real database for persistent user and key storage.
- [ ] Persist challenges across server restarts.
- [ ] Implement key rotation and revocation mechanisms.
- [ ] Improve private key storage security (client-side) beyond local file encryption.
- [ ] Add session management (issue secure tokens after login).
- [ ] Provide real HTTP server endpoints (e.g., using Spring Boot).
- [ ] Build an Android-compatible client library.
- [ ] Implement rate limiting and brute force protections.

## Usage Example

**Client Side Example:**

```java
KeyManager keyManager = new KeyManager();
NetworkClient networkClient = new NetworkClient();
AuthClient authClient = new AuthClient(keyManager, networkClient, "https://yourserver.com/auth");

authClient.signup("your_username");
authClient.logIn("your_username");
```

**Server Side Example:**

```java
UserManager userManager = new UserManager();
ChallengeManager challengeManager = new ChallengeManager(userManager);
AuthServer authServer = new AuthServer(userManager, challengeManager);

// Parse incoming JSON into Map<String, String>
// Call authServer.handleRequest(parsedRequestMap)
```

## Future Improvements

- Add database support for production-grade user storage
- Add session token generation after login
- Expire old challenges more aggressively
- Integrate with a real HTTP server (e.g., Spring Boot)
- Provide Android client library

## License

This project is licensed under the MIT License.
```